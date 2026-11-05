package com.coffeeshop.service;

import com.coffeeshop.dto.request.ShipmentUpdateRequest;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.ShipmentEvent;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.ShipmentStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.ShipmentEventRepository;
import com.coffeeshop.repository.ShipmentRepository;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Giao hàng do quán tự giao: chờ giao → đang giao → đã giao (hoặc giao thất bại). Đơn hàng đi theo: bắt đầu giao = đơn
 * "đang giao", đã giao = đơn hoàn tất (thu tiền mặt nếu chưa trả). Mỗi lần đổi trạng thái được ghi vào shipment_events.
 */
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository eventRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ShipmentResponse> findAll() {
        return shipmentRepository.findAllByOrderByIdDesc().stream().map(ShipmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse.Event> events(Long id) {
        get(id);
        return eventRepository.findByShipmentIdOrderByCreatedAtAscIdAsc(id).stream().map(ShipmentResponse.Event::from).toList();
    }

    @Transactional
    public ShipmentResponse update(Long id, ShipmentUpdateRequest request) {
        Shipment s = get(id);
        if (s.getStatus() == ShipmentStatus.DELIVERED || s.getStatus() == ShipmentStatus.FAILED) {
            throw new BadRequestException("Đơn giao đã kết thúc");
        }
        if (request.note() != null && !request.note().isBlank()) s.setNote(request.note().trim());
        Long orderId = s.getOrder().getId();
        switch (request.status()) {
            case DELIVERING -> {
                if (s.getStatus() == ShipmentStatus.DELIVERING) throw new BadRequestException("Đơn đang được giao");
                if (s.getOrder().getStatus() != OrderStatus.READY) throw new BadRequestException("Chỉ giao khi đồ uống đã pha xong");
                orderService.dispatch(orderId);
            }
            case DELIVERED -> {
                if (s.getStatus() != ShipmentStatus.DELIVERING) throw new BadRequestException("Đơn chưa được mang đi giao");
                orderService.complete(orderId);
            }
            case FAILED -> {
                if (s.getStatus() != ShipmentStatus.DELIVERING) throw new BadRequestException("Đơn chưa được mang đi giao");
                if (request.note() == null || request.note().isBlank()) throw new BadRequestException("Nhập lý do giao không thành công");
                s.setStatus(ShipmentStatus.FAILED);
                eventRepository.save(ShipmentEvent.builder().shipment(s).source("STAFF").status(ShipmentStatus.FAILED)
                        .note(request.note().trim()).createdBy(CurrentUser.username()).createdAt(LocalDateTime.now()).build());
                notificationService.notifyOrderChanged(OrderResponse.from(s.getOrder()));
            }
            default -> throw new BadRequestException("Trạng thái không hợp lệ");
        }
        return ShipmentResponse.from(s);
    }

    private Shipment get(Long id) {
        return shipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn giao: " + id));
    }
}
