package com.coffeeshop.stats.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name="cross_sell_items", uniqueConstraints=@UniqueConstraint(columnNames={"rule_id","coffee_id"}))
public class CrossSellItem {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="rule_id",nullable=false)
    private CrossSellRule rule;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="coffee_id",nullable=false)
    private Coffee coffee;

    @Column(nullable=false)
    private int position;
}
