package com.coffeeshop.stats.entity.repository;
import com.coffeeshop.stats.entity.MerchandisingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MerchandisingScheduleRepository extends JpaRepository<MerchandisingSchedule,Long> {
    List<MerchandisingSchedule> findAllByOrderByPriorityDescIdDesc();
}
