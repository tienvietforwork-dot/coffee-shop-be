package com.coffeeshop.service;

import com.coffeeshop.dto.request.DiningTableRequest;
import com.coffeeshop.dto.response.DiningTableResponse;
import com.coffeeshop.entity.DiningTable;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository tableRepository;

    @Transactional(readOnly = true)
    public List<DiningTableResponse> findAll() {
        return tableRepository.findAllByOrderByTableNoAsc().stream().map(DiningTableResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DiningTableResponse findByQr(String qrCode) {
        return DiningTableResponse.from(getByQr(qrCode));
    }

    @Transactional
    public DiningTableResponse create(DiningTableRequest request) {
        if (tableRepository.existsByTableNo(request.tableNo())) {
            throw new BadRequestException("Số bàn đã tồn tại: " + request.tableNo());
        }
        DiningTable table = DiningTable.builder()
                .qrCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .build();
        apply(table, request);
        return DiningTableResponse.from(tableRepository.save(table));
    }

    @Transactional
    public DiningTableResponse update(Long id, DiningTableRequest request) {
        DiningTable table = get(id);
        if (!table.getTableNo().equals(request.tableNo()) && tableRepository.existsByTableNo(request.tableNo())) {
            throw new BadRequestException("Số bàn đã tồn tại: " + request.tableNo());
        }
        apply(table, request);
        return DiningTableResponse.from(table);
    }

    /** Issues a new QR code, invalidating the old sticker. */
    @Transactional
    public DiningTableResponse regenerateQr(Long id) {
        DiningTable table = get(id);
        table.setQrCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        return DiningTableResponse.from(table);
    }

    @Transactional
    public void delete(Long id) {
        get(id).softDelete();
    }

    /**
     * Sơ đồ bàn: theo số bàn. Tầng / khu vực xếp theo bàn nhỏ nhất của nó (B01 ở tầng trệt thì tầng trệt đứng đầu),
     * không theo chữ cái — màn order nhóm theo thứ tự xuất hiện.
     */
    List<DiningTable> all() {
        return tableRepository.findAllByOrderByTableNoAsc();
    }

    DiningTable get(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn: " + id));
    }

    DiningTable getByQr(String qrCode) {
        return tableRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mã QR bàn không hợp lệ"));
    }

    private void apply(DiningTable table, DiningTableRequest request) {
        table.setTableNo(request.tableNo().trim());
        table.setFloor(blankToNull(request.floor()));
        table.setArea(blankToNull(request.area()));
        table.setCapacity(request.capacity());
        if (request.status() != null) table.setStatus(request.status());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
