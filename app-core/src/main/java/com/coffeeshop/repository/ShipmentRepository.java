package com.coffeeshop.repository;

import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @EntityGraph(attributePaths = {"order", "address", "shipper"})
    List<Shipment> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"order", "address", "shipper"})
    List<Shipment> findByIdIn(Collection<Long> ids);

    /** đơn của một người giao: đang chờ / đang giao, hoặc đã kết thúc từ {@code since} */
    @EntityGraph(attributePaths = {"order", "address", "shipper"})
    @Query("""
            select s from Shipment s where s.shipper.id = :staffId
              and (s.status in :open or s.updatedAt >= :since or s.deliveredAt >= :since)
            order by s.id desc""")
    List<Shipment> findForShipper(Long staffId, Collection<ShipmentStatus> open, LocalDateTime since);

    /** mọi đơn có người giao, phân công từ {@code since} hoặc chưa xong — để thống kê người giao */
    @EntityGraph(attributePaths = {"order", "shipper"})
    @Query("select s from Shipment s where s.shipper is not null and (s.assignedAt >= :since or s.status in :open)")
    List<Shipment> findAssignedSince(LocalDateTime since, Collection<ShipmentStatus> open);
}
