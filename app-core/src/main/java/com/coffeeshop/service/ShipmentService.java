package com.coffeeshop.service;

import com.coffeeshop.dto.request.ShipmentUpdateRequest;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.ShipmentEvent;
import com.coffeeshop.entity.Staff;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.ShipmentStatus;
import com.coffeeshop.entity.enums.WorkStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.integration.distance.DistanceGateway;
import com.coffeeshop.repository.ShipmentEventRepository;
import com.coffeeshop.repository.ShipmentRepository;
import com.coffeeshop.repository.StaffRepository;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.security.Perm;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Giao hàng do quán tự giao: chờ giao → đang giao → đã giao (hoặc giao thất bại). Đơn hàng đi theo: bắt đầu giao = đơn
 * "đang giao", đã giao = đơn hoàn tất (thu tiền mặt nếu chưa trả). Mỗi lần đổi trạng thái được ghi vào shipment_events.
 * <p>
 * Kiểm soát người giao: quản lý phân công đơn cho nhân viên; người giao chỉ thấy và cập nhật đơn của mình
 * (quyền "Đơn giao của tôi"); tuyến giao gợi ý thứ tự đi (điểm gần nhất trước) và vẽ đường trên bản đồ.
 */
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private static final Set<ShipmentStatus> OPEN = EnumSet.of(ShipmentStatus.PENDING, ShipmentStatus.DELIVERING);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository eventRepository;
    private final StaffRepository staffRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final DeliveryFeeService deliveryFeeService;
    private final DistanceGateway distance;

    @Transactional(readOnly = true)
    public List<ShipmentResponse> findAll() {
        return shipmentRepository.findAllByOrderByIdDesc().stream().map(ShipmentResponse::from).toList();
    }

    /** đơn của người giao đang đăng nhập: chưa xong + đã xong hôm nay */
    @Transactional(readOnly = true)
    public List<ShipmentResponse> mine() {
        Long staffId = requireStaff();
        return shipmentRepository.findForShipper(staffId, OPEN, LocalDate.now().atStartOfDay()).stream()
                .map(ShipmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse.Event> events(Long id) {
        get(id);
        return eventRepository.findByShipmentIdOrderByCreatedAtAscIdAsc(id).stream().map(ShipmentResponse.Event::from).toList();
    }

    /** phân công / đổi người giao (trước khi mang đi); {@code staffId} null = bỏ phân công */
    @Transactional
    public ShipmentResponse assign(Long id, Long staffId) {
        Shipment s = get(id);
        if (s.getStatus() != ShipmentStatus.PENDING) throw new BadRequestException("Chỉ đổi người giao khi đơn chưa mang đi giao");
        if (staffId == null) {
            s.setShipper(null);
            s.setAssignedAt(null);
            event(s, null, "Bỏ phân công người giao");
        } else {
            Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên: " + staffId));
            if (staff.getWorkStatus() != WorkStatus.ACTIVE) throw new BadRequestException(staff.getFullName() + " không còn làm việc");
            s.setShipper(staff);
            s.setAssignedAt(LocalDateTime.now());
            event(s, null, "Phân công " + staff.getFullName());
        }
        notificationService.notifyOrderChanged(OrderResponse.from(s.getOrder()));
        return ShipmentResponse.from(s);
    }

    @Transactional
    public ShipmentResponse update(Long id, ShipmentUpdateRequest request) {
        Shipment s = get(id);
        requireOwnerOrManager(s);
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
                event(s, ShipmentStatus.FAILED, request.note().trim());
                notificationService.notifyOrderChanged(OrderResponse.from(s.getOrder()));
            }
            default -> throw new BadRequestException("Trạng thái không hợp lệ");
        }
        return ShipmentResponse.from(s);
    }

    // ---- người giao -------------------------------------------------------------

    /**
     * Nhân viên đang làm có thể giao: số đơn đang cầm (chờ / đang giao), hôm nay đã giao / thất bại, thời gian giao
     * trung bình hôm nay (từ lúc mang đi tới lúc giao xong).
     */
    public record Shipper(Long id, String fullName, String phone, String position, int pending, int delivering,
                          int deliveredToday, int failedToday, Integer avgMinutesToday) {
    }

    @Transactional(readOnly = true)
    public List<Shipper> shippers() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        Map<Long, List<Shipment>> byStaff = shipmentRepository.findAssignedSince(today, OPEN).stream()
                .collect(Collectors.groupingBy(s -> s.getShipper().getId()));
        return staffRepository.findAll().stream().filter(st -> st.getWorkStatus() == WorkStatus.ACTIVE)
                .map(st -> {
                    List<Shipment> list = byStaff.getOrDefault(st.getId(), List.of());
                    int pending = count(list, ShipmentStatus.PENDING), delivering = count(list, ShipmentStatus.DELIVERING);
                    var done = list.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED && s.getDeliveredAt() != null
                            && !s.getDeliveredAt().isBefore(today)).toList();
                    int failed = (int) list.stream().filter(s -> s.getStatus() == ShipmentStatus.FAILED).count();
                    var minutes = done.stream().filter(s -> s.getPickedUpAt() != null)
                            .mapToLong(s -> Duration.between(s.getPickedUpAt(), s.getDeliveredAt()).toMinutes()).average();
                    return new Shipper(st.getId(), st.getFullName(), st.getPhone(), st.getPosition(), pending, delivering,
                            done.size(), failed, minutes.isPresent() ? (int) Math.round(minutes.getAsDouble()) : null);
                })
                // người đang cầm nhiều đơn lên đầu, rồi theo tên
                .sorted(Comparator.comparingInt((Shipper x) -> -(x.pending() + x.delivering())).thenComparing(Shipper::fullName))
                .toList();
    }

    private static int count(List<Shipment> list, ShipmentStatus status) {
        return (int) list.stream().filter(s -> s.getStatus() == status).count();
    }

    // ---- tuyến giao -------------------------------------------------------------

    public record Stop(Long shipmentId, String orderCode, String recipientName, String recipientPhone, String address,
                       Double lat, Double lng, ShipmentStatus status) {
    }

    /**
     * Tuyến giao gợi ý: xuất phát từ quán, đi điểm gần nhất trước. {@code path}: đường đi xe để vẽ (null = nối thẳng),
     * {@code unlocated}: địa chỉ không tìm được toạ độ (vẫn có trong link Google Maps).
     */
    public record RoutePlan(String shopAddress, Double shopLat, Double shopLng, List<Stop> stops, List<Stop> unlocated,
                            List<double[]> path, Double km, Integer minutes, String googleMapsUrl) {
    }

    @Transactional(readOnly = true)
    public RoutePlan route(List<Long> shipmentIds) {
        if (shipmentIds == null || shipmentIds.isEmpty()) throw new BadRequestException("Chọn ít nhất 1 đơn giao");
        List<Shipment> shipments = shipmentRepository.findByIdIn(shipmentIds);
        shipments.forEach(this::requireOwnerOrManager);
        var settings = deliveryFeeService.settings();
        DistanceGateway.Point shop = deliveryFeeService.shop();

        List<Stop> located = new ArrayList<>(), unlocated = new ArrayList<>();
        for (Shipment s : shipments) {
            DistanceGateway.Point p = deliveryFeeService.locateOrNull(s.getAddress().getAddress());
            Stop stop = new Stop(s.getId(), s.getOrder().getOrderCode(), s.getAddress().getRecipientName(),
                    s.getAddress().getRecipientPhone(), s.getAddress().getAddress(), p == null ? null : p.lat(), p == null ? null : p.lng(), s.getStatus());
            (p == null ? unlocated : located).add(stop);
        }
        // điểm gần nhất trước (đường chim bay) — đủ tốt cho vài đơn một chuyến
        List<Stop> ordered = new ArrayList<>();
        double lat = shop.lat(), lng = shop.lng();
        while (!located.isEmpty()) {
            final double fLat = lat, fLng = lng;
            Stop next = located.stream().min(Comparator.comparingDouble(x -> haversine(fLat, fLng, x.lat(), x.lng()))).orElseThrow();
            ordered.add(next);
            located.remove(next);
            lat = next.lat();
            lng = next.lng();
        }
        List<DistanceGateway.Point> points = new ArrayList<>();
        points.add(shop);
        ordered.forEach(x -> points.add(new DistanceGateway.Point(x.lat(), x.lng())));
        DistanceGateway.Path path = distance.path(points);
        // Google Maps hay không tìm ra địa chỉ chữ (tên cửa hàng, tên phường mới) → điểm nào đã có tọa độ thì gửi tọa độ
        List<String> all = new ArrayList<>(ordered.stream().map(x -> latLng(x.lat(), x.lng())).toList());
        all.addAll(unlocated.stream().map(Stop::address).toList());
        return new RoutePlan(settings.shopAddress(), shop.lat(), shop.lng(), ordered, unlocated,
                path == null ? null : path.line().stream().map(p -> new double[]{p.lat(), p.lng()}).toList(),
                path == null ? null : Math.round(path.km() * 10) / 10.0, path == null ? null : path.minutes(),
                googleMapsUrl(latLng(shop.lat(), shop.lng()), all));
    }

    static String latLng(double lat, double lng) {
        return String.format(java.util.Locale.ROOT, "%.6f,%.6f", lat, lng);
    }

    /** link Google Maps chỉ đường xe máy: quán → các điểm theo thứ tự (điểm cuối là đích, còn lại là điểm dừng); mỗi điểm là "lat,lng" hoặc địa chỉ chữ */
    static String googleMapsUrl(String origin, List<String> stops) {
        if (stops.isEmpty()) return null;
        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1&travelmode=two-wheeler");
        url.append("&origin=").append(enc(origin));
        url.append("&destination=").append(enc(stops.get(stops.size() - 1)));
        if (stops.size() > 1) url.append("&waypoints=").append(enc(String.join("|", stops.subList(0, stops.size() - 1))));
        return url.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ---- helpers ---------------------------------------------------------------

    /** người có quyền quản lý giao hàng làm được mọi đơn; người giao chỉ làm đơn được phân công cho mình */
    private void requireOwnerOrManager(Shipment s) {
        if (CurrentUser.principal() == null || CurrentUser.principal().getPermissions().contains(Perm.SHIPMENTS)) return;
        Long me = CurrentUser.staffId();
        if (me == null || s.getShipper() == null || !Objects.equals(s.getShipper().getId(), me)) {
            throw new AccessDeniedException("Đơn " + s.getOrder().getOrderCode() + " không được phân công cho bạn");
        }
    }

    private Long requireStaff() {
        Long id = CurrentUser.staffId();
        if (id == null) throw new BadRequestException("Tài khoản chưa gắn với nhân viên nào");
        return id;
    }

    private void event(Shipment s, ShipmentStatus status, String note) {
        eventRepository.save(ShipmentEvent.builder().shipment(s).source("STAFF").status(status).note(note)
                .createdBy(CurrentUser.username()).createdAt(LocalDateTime.now()).build());
    }

    private Shipment get(Long id) {
        return shipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn giao: " + id));
    }
}
