package com.coffeeshop.service;

import com.coffeeshop.dto.request.IncidentRequest;
import com.coffeeshop.dto.request.IncidentResolveRequest;
import com.coffeeshop.dto.response.IncidentResponse;
import com.coffeeshop.entity.Incident;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Voucher;
import com.coffeeshop.entity.enums.IncidentStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.IncidentRepository;
import com.coffeeshop.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Sự cố đơn hàng; khi cần đền bù, phát voucher cho khách (một sự cố có thể có nhiều voucher). */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IncidentRepository incidentRepository;
    private final VoucherRepository voucherRepository;
    private final OrderService orderService;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public List<IncidentResponse> findAll() {
        return incidentRepository.findAllByOrderByReportedAtDesc().stream()
                .map(i -> IncidentResponse.from(i, voucherRepository.findByIncidentId(i.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> findByOrder(Long orderId) {
        return incidentRepository.findByOrderIdOrderByReportedAtDesc(orderId).stream()
                .map(i -> IncidentResponse.from(i, voucherRepository.findByIncidentId(i.getId()))).toList();
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

        List<Voucher> vouchers = new ArrayList<>();
        if (request.voucherValue() != null) {
            if (incident.getOrder().getCustomer() == null) {
                throw new BadRequestException("Đơn không có thông tin khách hàng để phát voucher");
            }
            int count = request.voucherCount() == null ? 1 : request.voucherCount();
            int days = request.voucherValidDays() == null ? 30 : request.voucherValidDays();
            for (int i = 0; i < count; i++) {
                vouchers.add(voucherRepository.save(Voucher.builder()
                        .code(newCode())
                        .customer(incident.getOrder().getCustomer())
                        .incident(incident)
                        .discountValue(request.voucherValue())
                        .minOrderValue(request.voucherMinOrderValue() == null ? BigDecimal.ZERO : request.voucherMinOrderValue())
                        .issuedAt(now)
                        .expiresAt(now.plusDays(days))
                        .build()));
            }
        }
        return IncidentResponse.from(incident, vouchers.isEmpty() ? voucherRepository.findByIncidentId(id) : vouchers);
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("DB");
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (voucherRepository.existsByCode(code));
        return code;
    }
}
