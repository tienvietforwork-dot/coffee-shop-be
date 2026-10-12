package com.coffeeshop.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Entity
@Schema(title = "Thành viên nhóm khách hàng", description = "Liên kết khách hàng với nhóm và ghi nhận cách thêm thành viên")
@Table(name = "customer_group_members")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE customer_group_members SET del_flag = true WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerGroupMember extends BaseEntity {

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "is_manual", nullable = false)
    private Boolean isManual = false;

    @Column(name = "is_excluded", nullable = false)
    private Boolean isExcluded = false;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();
}
