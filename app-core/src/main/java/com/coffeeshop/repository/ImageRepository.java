package com.coffeeshop.repository;

import com.coffeeshop.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ImageRepository extends JpaRepository<Image, Long> {
    /** Soft delete without loading the image bytes. */
    @Modifying
    @Query("update Image i set i.delFlag = true, i.delUser = :user where i.id = :id")
    void softDelete(Long id, String user);
}
