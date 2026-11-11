package com.coffeeshop.crm.service;

import com.coffeeshop.crm.dto.request.FeedbackRequest;
import com.coffeeshop.crm.dto.request.FeedbackCloseRequest;
import com.coffeeshop.crm.dto.request.CustomerFeedbackReplyRequest;
import com.coffeeshop.crm.dto.request.FeedbackResponseRequest;
import com.coffeeshop.crm.dto.request.FeedbackApprovalRequest;
import com.coffeeshop.crm.dto.request.FeedbackUpdateRequest;
import com.coffeeshop.crm.dto.request.PublicFeedbackRequest;
import com.coffeeshop.crm.entity.Customer;
import com.coffeeshop.crm.entity.Feedback;
import com.coffeeshop.crm.entity.FeedbackResponse;
import com.coffeeshop.crm.exception.CrmNotFoundException;
import com.coffeeshop.crm.repository.CustomerRepository;
import com.coffeeshop.crm.repository.FeedbackRepository;
import com.coffeeshop.crm.repository.FeedbackResponseRepository;
import com.coffeeshop.crm.repository.CustomerPortalRepository;
import com.coffeeshop.crm.security.CrmPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashSet;
import java.util.List;

/** Xử lý góp ý/khiếu nại: tiếp nhận, phân loại, giao nhân viên, chuyển cấp, đóng và lưu câu trả lời. */
@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final CustomerRepository customerRepository;
    private final FeedbackResponseRepository responseRepository;
    private final CustomerPortalRepository customerPortalRepository;
    private final FeedbackSentimentAnalyzer sentimentAnalyzer;
    @PersistenceContext private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<Feedback> list(Long customerId, int page, int size) {
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new CrmNotFoundException("Không tìm thấy khách hàng ID " + customerId);
        }
        PageRequest pageable = PageRequest.of(page, size);
        return customerId == null ? feedbackRepository.findAllByDelFlagFalseOrderByCreatedAtDesc(pageable)
                : feedbackRepository.findAllByCustomerIdAndDelFlagFalseOrderByCreatedAtDesc(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Feedback get(Long id) { return find(id); }

    @Transactional
    public Feedback create(FeedbackRequest request) {
        requireCustomer(request.getCustomerId());
        requireStaff(request.getStaffId());
        Feedback feedback = new Feedback();
        feedback.setCustomerId(request.getCustomerId());
        feedback.setStaffId(request.getStaffId());
        feedback.setTopic(request.getTopic());
        feedback.setTopics(normalizeTopics(request.getTopics(), request.getTopic()));
        feedback.setIssueType(request.getIssueType());
        feedback.setCategory(request.getCategory());
        feedback.setSentiment(request.getSentiment() == null
                ? sentimentAnalyzer.analyze(request.getContent(), request.getRating()) : request.getSentiment());
        feedback.setContent(request.getContent());
        feedback.setRating(request.getRating() == null ? null : request.getRating().shortValue());
        feedback.setStatus("NEW");
        feedback.setEscalated(false);
        feedback.setApprovalStatus("NOT_REQUIRED");
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback createPublic(PublicFeedbackRequest request) {
        String phone = request.getPhone().trim();
        Customer customer = customerRepository.findByPhoneAndDelFlagFalse(phone).orElseGet(() -> {
            Customer newCustomer = new Customer();
            newCustomer.setFullName(request.getFullName() == null || request.getFullName().isBlank()
                    ? "Khách gửi góp ý" : request.getFullName().trim());
            newCustomer.setPhone(phone);
            return customerRepository.save(newCustomer);
        });

        FeedbackRequest feedbackRequest = new FeedbackRequest();
        feedbackRequest.setCustomerId(customer.getId());
        feedbackRequest.setTopic(request.getTopic() == null ? null : request.getTopic().trim());
        feedbackRequest.setContent(request.getContent().trim());
        return create(feedbackRequest);
    }

    @Transactional
    public Feedback update(Long id, FeedbackUpdateRequest request) {
        Feedback feedback = find(id);
        requireStaff(request.getStaffId());
        if (request.getStaffId() != null) feedback.setStaffId(request.getStaffId());
        if (request.getTopic() != null) feedback.setTopic(request.getTopic());
        if (request.getTopics() != null || request.getTopic() != null) {
            feedback.setTopics(normalizeTopics(request.getTopics(), request.getTopic()));
        }
        if (request.getIssueType() != null) feedback.setIssueType(request.getIssueType());
        if (request.getCategory() != null) feedback.setCategory(request.getCategory());
        if (request.getSentiment() != null) feedback.setSentiment(request.getSentiment());
        else if (request.getContent() != null) feedback.setSentiment(sentimentAnalyzer.analyze(request.getContent(),
                request.getRating() == null ? (feedback.getRating() == null ? null : feedback.getRating().intValue()) : request.getRating()));
        if (request.getContent() != null) feedback.setContent(request.getContent());
        if (request.getRating() != null) feedback.setRating(request.getRating().shortValue());
        if (request.getStatus() != null) {
            if ("ESCALATED".equals(request.getStatus()) && !"ESCALATED".equals(feedback.getStatus())) {
                throw new IllegalArgumentException("Dùng chức năng yêu cầu chuyển cấp để gửi duyệt");
            }
            feedback.setStatus(request.getStatus());
            feedback.setClosedAt("CLOSED".equals(request.getStatus()) || "RESOLVED".equals(request.getStatus())
                    ? java.time.LocalDateTime.now() : null);
        }
        if (request.getEscalated() != null) {
            if (request.getEscalated() && !Boolean.TRUE.equals(feedback.getEscalated())) throw new IllegalArgumentException("Dùng chức năng yêu cầu chuyển cấp để gửi duyệt");
            if ("PENDING".equals(feedback.getApprovalStatus())) throw new IllegalArgumentException("Yêu cầu chuyển cấp đang chờ quản lý duyệt");
            feedback.setEscalated(false);
        }
        if (request.getResolution() != null) feedback.setResolution(request.getResolution());
        if (request.getSatisfaction() != null) feedback.setSatisfaction(request.getSatisfaction());
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback requestEscalation(Long id, Long staffId) {
        Feedback feedback = find(id);
        requireStaff(staffId);
        if ("PENDING".equals(feedback.getApprovalStatus())) {
            throw new IllegalArgumentException("Yêu cầu chuyển cấp đang chờ duyệt");
        }
        if ("APPROVED".equals(feedback.getApprovalStatus())) {
            throw new IllegalArgumentException("Yêu cầu chuyển cấp đã được duyệt trước đó");
        }
        feedback.setStaffId(staffId == null ? feedback.getStaffId() : staffId);
        feedback.setEscalated(true);
        feedback.setStatus("ESCALATED");
        feedback.setApprovalStatus("PENDING");
        feedback.setApprovalNote(null);
        feedback.setApprovedBy(null);
        feedback.setApprovedAt(null);
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback decideEscalation(Long id, FeedbackApprovalRequest request, String actor) {
        Feedback feedback = find(id);
        if (!"PENDING".equals(feedback.getApprovalStatus())) {
            throw new IllegalArgumentException("Phản hồi này không có yêu cầu chuyển cấp đang chờ duyệt");
        }
        boolean approve = "APPROVE".equals(request.getDecision());
        if (!approve && (request.getNote() == null || request.getNote().isBlank())) {
            throw new IllegalArgumentException("Cần ghi lý do khi từ chối chuyển cấp");
        }
        feedback.setApprovalStatus(approve ? "APPROVED" : "REJECTED");
        feedback.setApprovalNote(request.getNote() == null ? null : request.getNote().trim());
        feedback.setApprovedBy(actor);
        feedback.setApprovedAt(java.time.LocalDateTime.now());
        if (!approve) {
            feedback.setEscalated(false);
            feedback.setStatus("IN_PROGRESS");
        }
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback close(Long id, FeedbackCloseRequest request) {
        Feedback feedback = find(id);
        if (request != null) {
            if (request.getResolution() != null) feedback.setResolution(request.getResolution());
            if (request.getSatisfaction() != null) feedback.setSatisfaction(request.getSatisfaction());
        }
        feedback.setStatus("CLOSED");
        feedback.setClosedAt(java.time.LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public FeedbackResponse recordResponse(Long feedbackId, FeedbackResponseRequest request) {
        find(feedbackId);
        requireStaff(request.getStaffId());
        FeedbackResponse response = new FeedbackResponse();
        response.setFeedbackId(feedbackId);
        response.setStaffId(request.getStaffId());
        response.setSenderType("STAFF");
        response.setMessage(request.getMessage().trim());
        response.setSentAt(java.time.LocalDateTime.now());
        return responseRepository.save(response);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getResponses(Long feedbackId) {
        find(feedbackId);
        return responseRepository.findAllByFeedbackIdAndDelFlagFalseOrderBySentAtAsc(feedbackId);
    }

    @Transactional
    public FeedbackResponse customerRespond(CrmPrincipal principal, Long feedbackId, CustomerFeedbackReplyRequest request) {
        Long customerId = resolveCustomerId(principal);
        Feedback feedback = feedbackRepository.findByIdAndCustomerIdAndDelFlagFalse(feedbackId, customerId)
                .orElseThrow(() -> new CrmNotFoundException("Không tìm thấy khiếu nại hoặc góp ý của bạn"));

        FeedbackResponse response = new FeedbackResponse();
        response.setFeedbackId(feedbackId);
        response.setStaffId(null);
        response.setSenderType("CUSTOMER");
        response.setMessage(request.getMessage().trim());
        response.setSentAt(java.time.LocalDateTime.now());
        response = responseRepository.save(response);

        // A customer reply puts a new or completed case back into the staff work queue.
        if ("NEW".equals(feedback.getStatus()) || "CLOSED".equals(feedback.getStatus())
                || "RESOLVED".equals(feedback.getStatus())) {
            feedback.setStatus("IN_PROGRESS");
            feedback.setClosedAt(null);
            feedbackRepository.save(feedback);
        }
        return response;
    }

    private Long resolveCustomerId(CrmPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Không xác định được tài khoản khách hàng; vui lòng đăng nhập lại");
        }
        return customerPortalRepository.findActiveCustomerIdByUserId(principal.userId())
                .filter(id -> id > 0)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tài khoản chưa được liên kết với hồ sơ khách hàng"));
    }

    @Transactional
    public void delete(Long id) {
        Feedback feedback = find(id);
        feedback.setDelFlag(true);
        feedback.setDelUser(null);
        feedbackRepository.save(feedback);
    }

    private Feedback find(Long id) {
        return feedbackRepository.findById(id).orElseThrow(() -> new CrmNotFoundException("Không tìm thấy phản hồi ID " + id));
    }

    private void requireCustomer(Long id) {
        if (!customerRepository.existsById(id)) throw new CrmNotFoundException("Không tìm thấy khách hàng ID " + id);
    }

    private void requireStaff(Long id) {
        if (id == null) return;
        Number count = (Number) entityManager.createNativeQuery("select count(*) from staff where id = :id and del_flag = false")
                .setParameter("id", id).getSingleResult();
        if (count.longValue() == 0) throw new CrmNotFoundException("Không tìm thấy nhân viên ID " + id);
    }

    private java.util.Set<String> normalizeTopics(List<String> topics, String legacyTopic) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (topics != null) {
            for (String value : topics) {
                if (value == null || value.isBlank()) continue;
                String topic = value.trim();
                if (topic.length() > 100) throw new IllegalArgumentException("Mỗi chủ đề tối đa 100 ký tự");
                normalized.add(topic);
            }
        }
        if (legacyTopic != null && !legacyTopic.isBlank()) normalized.add(legacyTopic.trim());
        if (normalized.size() > 10) throw new IllegalArgumentException("Tối đa 10 chủ đề cho mỗi phản hồi");
        return normalized;
    }
}
