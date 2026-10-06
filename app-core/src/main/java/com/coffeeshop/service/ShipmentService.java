package com.coffeeshop.service;

import com.coffeeshop.dto.request.ShipmentUpdateRequest;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.enums.ShipmentStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Điều phối giao hàng: đặt xe → tài xế nhận → đang giao → đã giao. */
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public List<ShipmentResponse> findAll() {
        return shipmentRepository.findAllByOrderByIdDesc().stream().map(ShipmentResponse::from).toList();
    }

    @Transactional
    public ShipmentResponse update(Long id, ShipmentUpdateRequest request) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn giao: " + id));
        if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.FAILED) {
            throw new BadRequestException("Đơn giao đã kết thúc");
        }
        if (request.carrier() != null) shipment.setCarrier(request.carrier());
        if (request.trackingCode() != null) shipment.setTrackingCode(request.trackingCode());
        if (request.note() != null) shipment.setNote(request.note());
        LocalDateTime now = LocalDateTime.now();
        switch (request.status()) {
            case BOOKED -> {
                if (shipment.getCarrier() == null || shipment.getCarrier().isBlank()) {
                    throw new BadRequestException("Cần nhập đơn vị vận chuyển khi đặt xe");
                }
                shipment.setBookedAt(now);
            }
            case DRIVER_ACCEPTED -> shipment.setDriverAcceptedAt(now);
            case DELIVERED -> shipment.setDeliveredAt(now);
            default -> {
            }
        }
        shipment.setStatus(request.status());
        if (request.status() == ShipmentStatus.DELIVERED) {
            orderService.completeDelivered(shipment.getOrder());
        }
        return ShipmentResponse.from(shipment);
    }
}
