package com.coffeeshop.repository;

import com.coffeeshop.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where a.createdAt >= :from and a.createdAt < :to
              and (:username is null or a.username = :username)
              and (:module is null or a.module = :module)
              and (:failed = false or a.statusCode >= 400)
              and (:keyword is null or lower(a.path) like :keyword or lower(a.detail) like :keyword)
            order by a.createdAt desc, a.id desc
            """)
    Page<AuditLog> search(LocalDateTime from, LocalDateTime to, String username, String module, boolean failed,
                          String keyword, Pageable page);

    @Query("select distinct a.username from AuditLog a where a.username is not null order by a.username")
    List<String> usernames();

    @Query("select distinct a.module from AuditLog a order by a.module")
    List<String> modules();

    @Modifying
    @Query("delete from AuditLog a where a.createdAt < :before")
    int deleteOlderThan(LocalDateTime before);
}
