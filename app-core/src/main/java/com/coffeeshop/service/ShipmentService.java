package com.coffeeshop.service;

import com.coffeeshop.dto.request.ShipmentAssignRequest;
import com.coffeeshop.dto.request.ShipmentStatusUpdateRequest;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.User;
import com.coffeeshop.entity.enums.ShipmentStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.ShipmentRepository;
import com.coffeeshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ShipmentResponse> findAll() {
        return shipmentRepository.findAll().stream().map(ShipmentResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShipmentResponse findById(Long id) {
        return ShipmentResponse.from(getEntity(id));
    }

    @Transactional
    public ShipmentResponse createForOrder(Long orderId, String address) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        Shipment shipment = Shipment.builder()
                .order(order)
                .address(address)
                .status(ShipmentStatus.PENDING)
                .build();
        return ShipmentResponse.from(shipmentRepository.save(shipment));
    }

    @Transactional
    public ShipmentResponse assignShipper(Long shipmentId, ShipmentAssignRequest request) {
        Shipment shipment = getEntity(shipmentId);
        User shipper = userRepository.findById(request.getShipperId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipper not found: " + request.getShipperId()));
        shipment.setShipper(shipper);
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            shipment.setAddress(request.getAddress());
        }
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        return ShipmentResponse.from(shipmentRepository.save(shipment));
    }

    @Transactional
    public ShipmentResponse updateStatus(Long shipmentId, ShipmentStatusUpdateRequest request) {
        Shipment shipment = getEntity(shipmentId);
        if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.FAILED) {
            throw new BadRequestException("Shipment is already finalized (" + shipment.getStatus() + ")");
        }
        shipment.setStatus(request.getStatus());
        if (request.getNote() != null) {
            shipment.setNote(request.getNote());
        }
        if (request.getStatus() == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(LocalDateTime.now());
        }
        return ShipmentResponse.from(shipmentRepository.save(shipment));
    }

    private Shipment getEntity(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));
    }
}
