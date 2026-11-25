package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.CustomerGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface CustomerGroupMemberRepository extends JpaRepository<CustomerGroupMember, Long> {
    @Query(value = """
            SELECT DISTINCT cgm.group_id
            FROM customer_group_members cgm
            JOIN users u ON u.customer_id = cgm.customer_id
            WHERE u.username = :username
              AND u.del_flag = FALSE
              AND cgm.del_flag = FALSE
              AND cgm.is_excluded = FALSE
            """, nativeQuery = true)
    Set<Long> findGroupIdsByUsername(@Param("username") String username);
}
