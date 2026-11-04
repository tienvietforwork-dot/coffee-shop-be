package com.coffeeshop.promotions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

@Setter
@Getter
@MappedSuperclass
public class BaseEntity {

    @Column(name = "created_by", nullable = false, length = 50)
    private String createBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createAt;

    @Column(name = "updated_by", nullable = false, length = 50)
    private String updateBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updateAt;

    @Column(name = "del_user", length = 50)
    private String delUser;

    @Column(name = "del_flag", nullable = false)
    private boolean delFlag;


    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        if (createBy == null || createBy.isBlank() || "system".equals(createBy)) createBy = getActor();
        if (createAt == null) createAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        LocalDateTime now = LocalDateTime.now();
        this.updateBy = getActor();
        this.updateAt = now;
    }

    private String getActor(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken){
            return "system";
        }

        String actor = authentication.getName();
        return (actor == null || actor.isBlank()) ? "system" : actor;
    }
}
