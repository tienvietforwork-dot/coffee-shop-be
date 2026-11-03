package com.coffeeshop.service;

import com.coffeeshop.dto.request.StocktakeRequest;
import com.coffeeshop.dto.response.StocktakeResponse;
import com.coffeeshop.entity.MaterialBatch;
import com.coffeeshop.entity.Stocktake;
import com.coffeeshop.entity.StocktakeItem;
import com.coffeeshop.entity.enums.BatchStatus;
import com.coffeeshop.entity.enums.StockQuality;
import com.coffeeshop.entity.enums.StocktakeStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.MaterialBatchRepository;
import com.coffeeshop.repository.StocktakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kiểm kê kho: lập phiếu (the lots to count, with their book quantity frozen) → nhập số đếm, phẩm chất, nguyên nhân
 * → cân bằng kho (thừa / thiếu become ADJUSTMENT movements; kém / mất phẩm chất is then written off with a phiếu hủy)
 * or hủy phiếu. Movements made while counting are kept: the lot moves by the counted difference, not to the count.
 */
@Service
@RequiredArgsConstructor
public class StocktakeService {

    private final StocktakeRepository stocktakeRepository;
    private final MaterialBatchRepository batchRepository;
    private final InventoryService inventoryService;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public List<StocktakeResponse> search(StocktakeStatus status) {
        return stocktakeRepository.search(status).stream().map(s -> StocktakeResponse.from(s, false)).toList();
    }

    @Transactional(readOnly = true)
    public StocktakeResponse find(Long id) {
        return StocktakeResponse.from(get(id), true);
    }

    @Transactional
    public StocktakeResponse create(StocktakeRequest request) {
        Stocktake s = stocktakeRepository.save(Stocktake.builder().code(newCode()).staff(staffService.current())
                .countedAt(LocalDateTime.now()).members(blankToNull(request.members())).note(blankToNull(request.note())).build());
        addLots(s, request.materialIds());
        if (s.getItems().isEmpty()) throw new BadRequestException("Không có lô nào còn hàng để kiểm");
        stocktakeRepository.flush();
        applyCounts(s, request.counts());
        return StocktakeResponse.from(s, true);
    }

    /** Save the counts typed so far (and add more materials). */
    @Transactional
    public StocktakeResponse update(Long id, StocktakeRequest request) {
        Stocktake s = draft(id);
        s.setMembers(blankToNull(request.members()));
        s.setNote(blankToNull(request.note()));
        if (request.materialIds() != null && !request.materialIds().isEmpty()) addLots(s, request.materialIds());
        stocktakeRepository.flush();
        applyCounts(s, request.counts());
        return StocktakeResponse.from(s, true);
    }

    /** Cân bằng kho: every line must be counted. */
    @Transactional
    public StocktakeResponse balance(Long id, String resolution) {
        Stocktake s = draft(id);
        var notCounted = s.getItems().stream().filter(i -> i.getCountedQuantity() == null).toList();
        if (!notCounted.isEmpty()) {
            throw new BadRequestException("Còn " + notCounted.size() + " lô chưa nhập số đếm (vd. "
                    + notCounted.get(0).getBatch().getMaterial().getName() + " lô #" + notCounted.get(0).getBatch().getId() + ")");
        }
        var staff = staffService.current();
        for (StocktakeItem item : s.getItems()) {
            BigDecimal diff = item.difference();
            if (diff.signum() == 0) continue;
            if (diff.signum() > 0 && (item.getReason() == null || item.getReason().isBlank())) {
                // thừa is unusual: still balanced, but the reason is required on the line
                throw new BadRequestException("Ghi nguyên nhân thừa của " + item.getBatch().getMaterial().getName());
            }
            inventoryService.adjustLot(item.getBatch(), diff, staff, "Kiểm kê " + s.getCode()
                    + (item.getReason() == null ? "" : " · " + item.getReason()));
        }
        s.setStatus(StocktakeStatus.BALANCED);
        s.setBalancedBy(staff);
        s.setBalancedAt(LocalDateTime.now());
        s.setResolution(blankToNull(resolution));
        return StocktakeResponse.from(s, true);
    }

    @Transactional
    public StocktakeResponse cancel(Long id) {
        Stocktake s = draft(id);
        s.setStatus(StocktakeStatus.CANCELLED);
        return StocktakeResponse.from(s, false);
    }

    // ---- helpers ---------------------------------------------------------

    private void addLots(Stocktake s, List<Long> materialIds) {
        Set<Long> already = s.getItems().stream().map(i -> i.getBatch().getId()).collect(Collectors.toCollection(HashSet::new));
        List<MaterialBatch> lots = batchRepository.findAll().stream()
                .filter(b -> b.getStatus() == BatchStatus.AVAILABLE && b.getRemainingQuantity().signum() > 0)
                .filter(b -> materialIds == null || materialIds.isEmpty() || materialIds.contains(b.getMaterial().getId()))
                .sorted((a, b) -> a.getMaterial().getName().compareToIgnoreCase(b.getMaterial().getName()) != 0
                        ? a.getMaterial().getName().compareToIgnoreCase(b.getMaterial().getName()) : Long.compare(a.getId(), b.getId()))
                .toList();
        for (MaterialBatch b : lots) {
            if (!already.add(b.getId())) continue;
            s.getItems().add(StocktakeItem.builder().stocktake(s).batch(b).bookQuantity(b.getRemainingQuantity())
                    .unitCost(b.getUnitCost()).build());
        }
    }

    private void applyCounts(Stocktake s, List<StocktakeRequest.Count> counts) {
        if (counts == null) return;
        Map<Long, StocktakeItem> lines = s.getItems().stream().collect(Collectors.toMap(StocktakeItem::getId, Function.identity()));
        for (StocktakeRequest.Count c : counts) {
            StocktakeItem item = lines.get(c.itemId());
            if (item == null) throw new BadRequestException("Dòng #" + c.itemId() + " không thuộc phiếu " + s.getCode());
            if (c.countedQuantity() != null && c.countedQuantity().compareTo(item.getBatch().getImportQuantity()) > 0) {
                throw new BadRequestException(item.getBatch().getMaterial().getName() + " lô #" + item.getBatch().getId()
                        + ": số đếm lớn hơn số đã nhập của lô");
            }
            item.setCountedQuantity(c.countedQuantity());
            item.setQuality(c.quality() == null ? StockQuality.GOOD : c.quality());
            item.setReason(blankToNull(c.reason()));
        }
    }

    private Stocktake get(Long id) {
        return stocktakeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu kiểm kê: " + id));
    }

    private Stocktake draft(Long id) {
        Stocktake s = get(id);
        if (s.getStatus() != StocktakeStatus.DRAFT) throw new BadRequestException("Phiếu đã cân bằng hoặc đã hủy");
        return s;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** KK + yyMMdd + running number of the day */
    private String newCode() {
        String prefix = "KK" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        return prefix + String.format("%03d", stocktakeRepository.countByCodeStartingWith(prefix) + 1);
    }
}
