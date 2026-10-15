package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.FeedbackResponse;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackResponseRepository extends JpaRepository<FeedbackResponse, Long> {
    List<FeedbackResponse> findAllByFeedbackIdAndDelFlagFalseOrderBySentAtAsc(Long feedbackId);
    List<FeedbackResponse> findAllByFeedbackIdInAndDelFlagFalseOrderBySentAtAsc(Collection<Long> feedbackIds);
    Optional<FeedbackResponse> findByIdAndFeedbackIdAndDelFlagFalse(Long id, Long feedbackId);
}
