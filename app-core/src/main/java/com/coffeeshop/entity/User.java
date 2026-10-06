package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE users SET del_flag = true WHERE id = ?")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", length = 150)
    private String fullName;
    @Column(length = 150)
    private String email;
    @Column(length = 30)
    private String phone;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
    /** Set for staff accounts: the employee record this login belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    /** Set for customer accounts. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    private List<UserRole> userRoles = new ArrayList<>();

    public List<Role> getRoles() {
        return userRoles.stream().map(UserRole::getRole).toList();
    }
}
