package com.coffeeshop.crm.service;

import com.coffeeshop.crm.dto.request.InteractionRequest;
import com.coffeeshop.crm.dto.request.InteractionCloseRequest;
import com.coffeeshop.crm.dto.request.InteractionUpdateRequest;
import com.coffeeshop.crm.entity.Interaction;
import com.coffeeshop.crm.exception.CrmNotFoundException;
import com.coffeeshop.crm.repository.CustomerRepository;
import com.coffeeshop.crm.repository.InteractionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InteractionService {
    private final InteractionRepository interactionRepository;
    private final CustomerRepository customerRepository;
    @PersistenceContext private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<Interaction> list(Long customerId, int page, int size) {
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new CrmNotFoundException("Không tìm thấy khách hàng ID " + customerId);
        }
        PageRequest pageable = PageRequest.of(page, size);
        return customerId == null ? interactionRepository.findAllByDelFlagFalseOrderByOccurredAtDesc(pageable)
                : interactionRepository.findAllByCustomerIdAndDelFlagFalseOrderByOccurredAtDesc(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Interaction> dueReminders(java.time.LocalDateTime until, int page, int size) {
        return interactionRepository.findDueReminders(until, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Interaction get(Long id) { return find(id); }

    @Transactional
    public Interaction create(InteractionRequest request) {
        requireCustomer(request.getCustomerId());
        requireStaff(request.getStaffId());
        Interaction interaction = new Interaction();
        interaction.setCustomerId(request.getCustomerId());
        interaction.setChannel(request.getChannel());
        interaction.setContent(request.getContent());
        interaction.setOccurredAt(request.getOccurredAt() == null ? java.time.LocalDateTime.now() : request.getOccurredAt());
        interaction.setNote(request.getNote());
        interaction.setStaffId(request.getStaffId());
        interaction.setRemindAt(request.getRemindAt());
        interaction.setVisibleToCustomer(Boolean.TRUE.equals(request.getVisibleToCustomer()));
        interaction.setStatus(request.getRemindAt() == null ? "OPEN" : "FOLLOW_UP");
        return interactionRepository.save(interaction);
    }

    @Transactional
    public Interaction update(Long id, InteractionUpdateRequest request) {
        Interaction interaction = find(id);
        requireStaff(request.getStaffId());
        if (request.getChannel() != null) interaction.setChannel(request.getChannel());
        if (request.getContent() != null) interaction.setContent(request.getContent());
        if (request.getOccurredAt() != null) interaction.setOccurredAt(request.getOccurredAt());
        if (request.getOutcome() != null) interaction.setOutcome(request.getOutcome());
        if (request.getVisibleToCustomer() != null) interaction.setVisibleToCustomer(request.getVisibleToCustomer());
        if (request.getNote() != null) interaction.setNote(request.getNote());
        if (request.getStaffId() != null) interaction.setStaffId(request.getStaffId());
        if (Boolean.TRUE.equals(request.getClearReminder()) && request.getRemindAt() != null) {
            throw new IllegalArgumentException("Không thể gửi remindAt đồng thời với clearReminder=true");
        }
        if (Boolean.TRUE.equals(request.getClearReminder())) {
            interaction.setRemindAt(null);
            if (request.getStatus() == null && "FOLLOW_UP".equals(interaction.getStatus())) {
                interaction.setStatus("OPEN");
            }
        }
        if (request.getRemindAt() != null) {
            interaction.setRemindAt(request.getRemindAt());
            if (request.getStatus() == null && !"CLOSED".equals(interaction.getStatus())) interaction.setStatus("FOLLOW_UP");
        }
        if (request.getStatus() != null) {
            interaction.setStatus(request.getStatus());
            interaction.setClosedAt("CLOSED".equals(request.getStatus()) ? java.time.LocalDateTime.now() : null);
            if ("CLOSED".equals(request.getStatus())) interaction.setRemindAt(null);
        }
        return interactionRepository.save(interaction);
    }

    @Transactional
    public Interaction close(Long id, InteractionCloseRequest request) {
        Interaction interaction = find(id);
        interaction.setStatus("CLOSED");
        interaction.setClosedAt(java.time.LocalDateTime.now());
        interaction.setRemindAt(null);
        if (request != null && request.getOutcome() != null) interaction.setOutcome(request.getOutcome().trim());
        return interactionRepository.save(interaction);
    }

    @Transactional
    public void delete(Long id) {
        Interaction interaction = find(id);
        interaction.setDelFlag(true);
        interaction.setDelUser(null);
        interactionRepository.save(interaction);
    }

    private Interaction find(Long id) {
        return interactionRepository.findById(id).orElseThrow(() -> new CrmNotFoundException("Không tìm thấy tương tác ID " + id));
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
}
