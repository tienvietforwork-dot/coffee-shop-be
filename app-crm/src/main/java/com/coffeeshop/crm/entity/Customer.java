package com.coffeeshop.crm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Schema(title = "Hồ sơ khách hàng", description = "Thông tin nhận diện cơ bản và các thuộc tính của khách hàng")
@Table(name = "customers")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE customers SET del_flag = true WHERE id = ?")
@Getter @Setter
public class Customer extends BaseEntity {

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone", length = 15, nullable = false)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();
}
