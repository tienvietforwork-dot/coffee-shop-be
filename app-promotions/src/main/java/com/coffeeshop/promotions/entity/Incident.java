package com.coffeeshop.promotions.entity;

import com.coffeeshop.promotions.entity.enums.IncidentType;
import com.coffeeshop.promotions.entity.enums.IndentSeverity;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private IndentSeverity severity;
}
