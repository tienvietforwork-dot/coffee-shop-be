package com.coffeeshop.crm.service;

import com.coffeeshop.crm.dto.response.CustomerPortalFeedbackResponse;
import com.coffeeshop.crm.dto.response.CustomerPortalInteractionResponse;
import com.coffeeshop.crm.dto.response.CustomerPortalReadReceiptResponse;
import com.coffeeshop.crm.entity.Feedback;
import com.coffeeshop.crm.entity.FeedbackResponse;
import com.coffeeshop.crm.exception.CrmNotFoundException;
import com.coffeeshop.crm.repository.CustomerPortalRepository;
import com.coffeeshop.crm.repository.FeedbackRepository;
import com.coffeeshop.crm.repository.FeedbackResponseRepository;
import com.coffeeshop.crm.repository.InteractionRepository;
import com.coffeeshop.crm.security.CrmPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerPortalService {
    private final CustomerPortalRepository customerPortalRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackResponseRepository feedbackResponseRepository;
    private final InteractionRepository interactionRepository;

    @Transactional(readOnly = true)
    public Page<CustomerPortalFeedbackResponse> feedbacks(CrmPrincipal principal, int page, int size) {
        Long customerId = resolveCustomerId(principal);
        Page<com.coffeeshop.crm.entity.Feedback> feedbacks = feedbackRepository
                .findAllByCustomerIdAndDelFlagFalseOrderByCreatedAtDesc(customerId, PageRequest.of(page, size));
        if (feedbacks.isEmpty()) return feedbacks.map(feedback -> CustomerPortalFeedbackResponse.from(feedback, List.of()));

        List<Long> feedbackIds = feedbacks.getContent().stream()
                .map(com.coffeeshop.crm.entity.Feedback::getId).toList();
        Map<Long, List<FeedbackResponse>> repliesByFeedback = feedbackResponseRepository
                .findAllByFeedbackIdInAndDelFlagFalseOrderBySentAtAsc(feedbackIds).stream()
                .collect(Collectors.groupingBy(FeedbackResponse::getFeedbackId));

        return feedbacks.map(feedback -> CustomerPortalFeedbackResponse.from(
                feedback, repliesByFeedback.getOrDefault(feedback.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public Page<CustomerPortalInteractionResponse> interactions(CrmPrincipal principal, int page, int size) {
        Long customerId = resolveCustomerId(principal);
        return interactionRepository
                .findAllByCustomerIdAndVisibleToCustomerTrueAndDelFlagFalseOrderByOccurredAtDesc(
                        customerId, PageRequest.of(page, size))
                .map(CustomerPortalInteractionResponse::from);
    }

    @Transactional
    public CustomerPortalReadReceiptResponse markResponseRead(CrmPrincipal principal, Long feedbackId, Long responseId) {
        Long customerId = resolveCustomerId(principal);
        Feedback feedback = feedbackRepository.findByIdAndCustomerIdAndDelFlagFalse(feedbackId, customerId)
                .orElseThrow(() -> new CrmNotFoundException("Không tìm thấy phản hồi của khách hàng"));
        FeedbackResponse response = feedbackResponseRepository
                .findByIdAndFeedbackIdAndDelFlagFalse(responseId, feedback.getId())
                .orElseThrow(() -> new CrmNotFoundException("Không tìm thấy câu trả lời"));

        if (response.getReadAt() == null) {
            response.setReadAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
            response = feedbackResponseRepository.save(response);
        }
        return new CustomerPortalReadReceiptResponse(feedback.getId(), response.getId(), response.getReadAt());
    }

    private Long resolveCustomerId(CrmPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Không xác định được tài khoản khách hàng; vui lòng đăng nhập lại");
        }
        return customerPortalRepository.findActiveCustomerIdByUserId(principal.userId())
                .filter(id -> id > 0)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản chưa được liên kết với hồ sơ khách hàng"));
    }
}
