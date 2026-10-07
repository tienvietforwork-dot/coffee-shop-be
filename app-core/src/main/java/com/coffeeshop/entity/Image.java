package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Uploaded picture (coffee photos…) stored in the database so it survives redeploys; served by {@code /api/public/images/{id}}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "images")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE images SET del_flag = true WHERE id = ?")
public class Image extends BaseEntity {
    /** Public path an uploaded image is served from (ImageController); relative to the API origin. */
    public static final String URL_PREFIX = "/api/public/images/";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;
    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] data;
}
