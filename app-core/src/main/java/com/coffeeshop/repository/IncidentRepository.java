package com.coffeeshop.repository;

import com.coffeeshop.entity.Incident;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    @EntityGraph(attributePaths = {"order", "handledBy"})
    List<Incident> findAllByOrderByReportedAtDesc();

    List<Incident> findByOrderIdOrderByReportedAtDesc(Long orderId);
}
