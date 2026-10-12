package com.coffeeshop.crm.service;

import com.coffeeshop.crm.dto.request.CriteriaRuleDTO;
import com.coffeeshop.crm.dto.request.CustomerGroupRequest;
import com.coffeeshop.crm.entity.Customer;
import com.coffeeshop.crm.entity.CustomerGroup;
import com.coffeeshop.crm.entity.CustomerGroupMember;
import com.coffeeshop.crm.exception.CrmConflictException;
import com.coffeeshop.crm.exception.CrmNotFoundException;
import com.coffeeshop.crm.repository.CustomerAnalyticsRepository;
import com.coffeeshop.crm.repository.CustomerGroupMemberRepository;
import com.coffeeshop.crm.repository.CustomerGroupRepository;
import com.coffeeshop.crm.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerGroupService {
    private final CustomerGroupRepository groupRepository;
    private final CustomerGroupMemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAnalyticsRepository analyticsRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CustomerGroup> getGroups() {
        return groupRepository.findAllByDelFlagFalseOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public CustomerGroup getGroup(Long groupId) {
        return findGroup(groupId);
    }

    @Transactional(readOnly = true)
    public List<CustomerGroupMember> getMembers(Long groupId) {
        findGroup(groupId);
        return memberRepository.findAllByGroupIdAndDelFlagFalseAndIsExcludedFalseOrderByAddedAtDesc(groupId);
    }

    @Transactional
    public CustomerGroup createGroup(CustomerGroupRequest request) {
        validateRules(request.getGroupType(), request.getCriteriaRule());
        String name = request.getName().trim();
        if (groupRepository.existsByNameIgnoreCaseAndDelFlagFalse(name)) {
            throw new CrmConflictException("Tên nhóm khách hàng đã tồn tại");
        }
        CustomerGroup group = new CustomerGroup();
        group.setName(name);
        group.setDescription(request.getDescription());
        group.setGroupType(request.getGroupType());
        group.setCriteriaRule(request.getCriteriaRule() == null ? null : objectMapper.valueToTree(request.getCriteriaRule()));
        return groupRepository.save(group);
    }

    @Transactional
    public CustomerGroup updateGroup(Long groupId, CustomerGroupRequest request) {
        validateRules(request.getGroupType(), request.getCriteriaRule());
        CustomerGroup group = findGroup(groupId);
        String name = request.getName().trim();
        if (groupRepository.existsByNameIgnoreCaseAndDelFlagFalseAndIdNot(name, groupId)) {
            throw new CrmConflictException("Tên nhóm khách hàng đã tồn tại");
        }
        if ("AUTOMATIC".equals(group.getGroupType()) && "MANUAL".equals(request.getGroupType())) {
            memberRepository.softDeleteAutomaticMembers(groupId);
        }
        group.setName(name);
        group.setDescription(request.getDescription());
        group.setGroupType(request.getGroupType());
        group.setCriteriaRule(request.getCriteriaRule() == null ? null : objectMapper.valueToTree(request.getCriteriaRule()));
        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<Customer> preview(Long groupId) {
        CustomerGroup group = findGroup(groupId);
        if (!"AUTOMATIC".equals(group.getGroupType())) {
            throw new IllegalArgumentException("Chỉ xem trước được nhóm AUTOMATIC");
        }
        return findMatches(readRules(group));
    }

    @Transactional
    public int evaluateAutomaticGroup(Long groupId) {
        CustomerGroup group = findGroup(groupId);
        if (!"AUTOMATIC".equals(group.getGroupType())) {
            throw new IllegalArgumentException("Chỉ tính lại được nhóm AUTOMATIC");
        }
        List<Customer> matchingCustomers = findMatches(readRules(group));
        memberRepository.softDeleteAutomaticMembers(groupId);
        for (Customer customer : matchingCustomers) {
            if (!memberRepository.existsByGroupIdAndCustomerIdAndDelFlagFalse(groupId, customer.getId())) {
                CustomerGroupMember member = new CustomerGroupMember();
                member.setGroupId(groupId);
                member.setCustomerId(customer.getId());
                member.setIsManual(false);
                memberRepository.save(member);
            }
        }
        group.setRecalculatedAt(LocalDateTime.now());
        groupRepository.save(group);
        return matchingCustomers.size();
    }

    @Transactional
    public CustomerGroupMember addMember(Long groupId, Long customerId) {
        findGroup(groupId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CrmNotFoundException("Không tìm thấy khách hàng ID " + customerId));
        return memberRepository.findByGroupIdAndCustomerIdAndDelFlagFalse(groupId, customerId)
                .map(existing -> {
                    existing.setIsManual(true);
                    existing.setIsExcluded(false);
                    return existing;
                })
                .orElseGet(() -> {
                    CustomerGroupMember member = new CustomerGroupMember();
                    member.setGroupId(groupId);
                    member.setCustomerId(customer.getId());
                    member.setIsManual(true);
                    member.setIsExcluded(false);
                    return memberRepository.save(member);
                });
    }

    @Transactional
    public void removeMember(Long groupId, Long customerId) {
        CustomerGroup group = findGroup(groupId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CrmNotFoundException("Không tìm thấy khách hàng ID " + customerId));
        CustomerGroupMember member = memberRepository.findByGroupIdAndCustomerIdAndDelFlagFalse(groupId, customerId).orElse(null);
        if ("AUTOMATIC".equals(group.getGroupType())) {
            if (member == null) {
                member = new CustomerGroupMember();
                member.setGroupId(groupId);
                member.setCustomerId(customer.getId());
            }
            member.setIsManual(true);
            member.setIsExcluded(true);
            memberRepository.save(member);
        } else {
            if (member == null) throw new CrmNotFoundException("Khách hàng không nằm trong nhóm này");
            member.setDelFlag(true);
            member.setDelUser(null);
            memberRepository.save(member);
        }
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        CustomerGroup group = findGroup(groupId);
        memberRepository.softDeleteGroupMembers(groupId);
        group.setDelFlag(true);
        group.setDelUser(null);
        groupRepository.save(group);
    }

    private List<Customer> findMatches(CriteriaRuleDTO rule) {
        List<Long> ids = analyticsRepository.matchingCustomerIds(rule.getMinSpend(), rule.getMaxSpend(),
                rule.getMinOrders(), rule.getLastOrderDays(), rule.getFavoriteCategoryId(), rule.getFavoriteCoffeeId());
        if (ids.isEmpty()) return List.of();
        return customerRepository.findAllById(ids);
    }

    private CriteriaRuleDTO readRules(CustomerGroup group) {
        if (group.getCriteriaRule() == null || group.getCriteriaRule().isNull()) {
            throw new IllegalArgumentException("Nhóm tự động chưa có tiêu chí");
        }
        CriteriaRuleDTO rules = objectMapper.convertValue(group.getCriteriaRule(), CriteriaRuleDTO.class);
        validateRules(group.getGroupType(), rules);
        return rules;
    }

    private void validateRules(String groupType, CriteriaRuleDTO rules) {
        if ("AUTOMATIC".equals(groupType)) {
            if (rules == null) throw new IllegalArgumentException("Nhóm AUTOMATIC cần có ít nhất một tiêu chí");
            if (rules.getMinSpend() == null && rules.getMaxSpend() == null && rules.getMinOrders() == null &&
                    rules.getLastOrderDays() == null && rules.getFavoriteCategoryId() == null &&
                    rules.getFavoriteCoffeeId() == null) {
                throw new IllegalArgumentException("Nhóm AUTOMATIC cần có ít nhất một tiêu chí");
            }
            if (rules.getMinSpend() != null && rules.getMaxSpend() != null && rules.getMinSpend().compareTo(rules.getMaxSpend()) > 0) {
                throw new IllegalArgumentException("minSpend không được lớn hơn maxSpend");
            }
            if (rules.getMinSpend() != null && rules.getMinSpend().signum() < 0 ||
                    rules.getMaxSpend() != null && rules.getMaxSpend().signum() < 0 ||
                    rules.getMinOrders() != null && rules.getMinOrders() < 1 ||
                    rules.getLastOrderDays() != null && rules.getLastOrderDays() < 1 ||
                    rules.getFavoriteCategoryId() != null && rules.getFavoriteCategoryId() < 1 ||
                    rules.getFavoriteCoffeeId() != null && rules.getFavoriteCoffeeId() < 1) {
                throw new IllegalArgumentException("Min/max spend phải không âm; số đơn, số ngày và ID phải lớn hơn 0");
            }
        } else if (rules != null) {
            throw new IllegalArgumentException("Nhóm MANUAL không nhận tiêu chí tự động");
        }
    }

    private CustomerGroup findGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new CrmNotFoundException("Không tìm thấy nhóm khách hàng ID " + groupId));
    }
}
