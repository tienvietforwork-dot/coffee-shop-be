package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalTime;

/** Ca làm việc mẫu (ca sáng 6:00–12:00, ca chiều, ca tối…). A split shift (ca gãy) is two shifts on the same day. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "work_shifts")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE work_shifts SET del_flag = true WHERE id = ?")
public class WorkShift extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String name;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    /** đi muộn / về sớm trong số phút này không bị tính */
    @Builder.Default
    @Column(name = "grace_minutes", nullable = false)
    private int graceMinutes = 5;
    @Column(length = 20)
    private String color;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** minutes of the shift (ends after midnight allowed) */
    public long minutes() {
        long m = java.time.Duration.between(startTime, endTime).toMinutes();
        return m <= 0 ? m + 24 * 60 : m;
    }
}
