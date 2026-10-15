package com.coffeeshop.crm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Schema(title = "Địa chỉ khách hàng", description = "Địa chỉ nhận hàng hoặc liên hệ của khách hàng")
@Table(name = "customer_addresses")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE customer_addresses SET del_flag = true WHERE id = ?")
@Getter @Setter
public class CustomerAddress extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "label", length = 30)
    private String label; // nhà / công ty

    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", length = 15, nullable = false)
    private String recipientPhone;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
