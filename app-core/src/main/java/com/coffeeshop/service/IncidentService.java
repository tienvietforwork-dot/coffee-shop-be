package com.coffeeshop.service;

import com.coffeeshop.dto.request.IncidentRequest;
import com.coffeeshop.dto.request.IncidentResolveRequest;
import com.coffeeshop.dto.response.IncidentResponse;
import com.coffeeshop.dto.response.VoucherOptionResponse;
import com.coffeeshop.entity.Incident;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Voucher;
import com.coffeeshop.entity.VoucherDetail;
import com.coffeeshop.entity.enums.IncidentStatus;
import com.coffeeshop.entity.enums.VoucherTypeStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.IncidentRepository;
import com.coffeeshop.repository.VoucherDetailRepository;
import com.coffeeshop.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sự cố đơn hàng; khi cần đền bù, phát voucher của một loại đã chọn cho khách (một sự cố có thể có nhiều voucher).
 * Mỗi voucher phát ra giữ 1 lượt của loại đó; không đủ lượt thì không phát.
 */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IncidentRepository incidentRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherDetailRepository voucherDetailRepository;
    private final OrderService orderService;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public List<IncidentResponse> findAll() {
        return incidentRepository.findAllByOrderByReportedAtDesc().stream()
                .map(i -> IncidentResponse.from(i, voucherDetailRepository.findByIncidentId(i.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> findByOrder(Long orderId) {
        return incidentRepository.findByOrderIdOrderByReportedAtDesc(orderId).stream()
                .map(i -> IncidentResponse.from(i, voucherDetailRepository.findByIncidentId(i.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherOptionResponse> voucherOptions() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findByStatusOrderByNameAsc(VoucherTypeStatus.ACTIVE).stream()
                .map(v -> new VoucherOptionResponse(v.getId(), v.getName(), v.getDiscountValue(), v.getMinOrderValue(),
                        v.getQuantity(), v.getValidDays(), remaining(v, now)))
                .toList();
    }

    @Transactional
    public IncidentResponse report(Long orderId, IncidentRequest request) {
        Order order = orderService.get(orderId);
        Incident incident = incidentRepository.save(Incident.builder()
                .order(order).type(request.type()).severity(request.severity())
                .reason(request.reason()).description(request.description())
                .build());
        return IncidentResponse.from(incident, List.of());
    }

    @Transactional
    public IncidentResponse resolve(Long id, IncidentResolveRequest request) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự cố: " + id));
        if (incident.getStatus() == IncidentStatus.RESOLVED) throw new BadRequestException("Sự cố đã được xử lý");
        LocalDateTime now = LocalDateTime.now();
        incident.setHandledBy(staffService.current());
        incident.setResolution(request.resolution().trim());
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(now);

        List<VoucherDetail> vouchers = new ArrayList<>();
        if (request.voucherId() != null) {
            if (incident.getOrder().getCustomer() == null) {
                throw new BadRequestException("Đơn không có thông tin khách hàng để phát voucher");
            }
            Voucher voucher = voucherRepository.lockById(request.voucherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher: " + request.voucherId()));
            if (voucher.getStatus() != VoucherTypeStatus.ACTIVE) throw new BadRequestException("Voucher đã ngừng phát");
            int count = request.voucherCount() == null ? 1 : request.voucherCount();
            long left = remaining(voucher, now);
            if (left < count) {
                throw new BadRequestException("Voucher \"" + voucher.getName() + "\" chỉ còn " + left + " lượt");
            }
            Integer days = request.voucherValidDays() != null ? request.voucherValidDays() : voucher.getValidDays();
            for (int i = 0; i < count; i++) {
                vouchers.add(voucherDetailRepository.save(VoucherDetail.builder()
                        .voucher(voucher)
                        .code(newCode())
                        .customer(incident.getOrder().getCustomer())
                        .incident(incident)
                        .issuedAt(now)
                        .expiresAt(days == null ? null : now.plusDays(days))
                        .build()));
            }
        }
        return IncidentResponse.from(incident, vouchers.isEmpty() ? voucherDetailRepository.findByIncidentId(id) : vouchers);
    }

    private long remaining(Voucher voucher, LocalDateTime now) {
        return Math.max(0, voucher.getQuantity() - voucherDetailRepository.countTakenSlots(voucher.getId(), now));
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("DB");
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (voucherDetailRepository.existsByCode(code));
        return code;
    }
}
