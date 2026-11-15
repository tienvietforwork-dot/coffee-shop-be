package com.coffeeshop.crm.service;

import com.coffeeshop.crm.dto.request.CriteriaRuleDTO;
import com.coffeeshop.crm.dto.request.CustomerGroupRequest;
import com.coffeeshop.crm.entity.CustomerGroup;
import com.coffeeshop.crm.entity.CustomerGroupMember;
import com.coffeeshop.crm.entity.Customer;
import com.coffeeshop.crm.repository.CustomerAnalyticsRepository;
import com.coffeeshop.crm.repository.CustomerGroupMemberRepository;
import com.coffeeshop.crm.repository.CustomerGroupRepository;
import com.coffeeshop.crm.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerGroupServiceTest {
    @Mock CustomerGroupRepository groupRepository;
    @Mock CustomerGroupMemberRepository memberRepository;
    @Mock CustomerRepository customerRepository;
    @Mock CustomerAnalyticsRepository analyticsRepository;
    @Mock ObjectMapper objectMapper;
    @InjectMocks CustomerGroupService groupService;

    @Test
    void automaticGroupRequiresAtLeastOneRule() {
        CustomerGroupRequest request = request("AUTOMATIC", null);

        assertThrows(IllegalArgumentException.class, () -> groupService.createGroup(request));
        verify(groupRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void manualGroupRejectsAutomaticRules() {
        CustomerGroupRequest request = request("MANUAL", CriteriaRuleDTO.builder().minOrders(2).build());

        assertThrows(IllegalArgumentException.class, () -> groupService.createGroup(request));
        verify(groupRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void automaticGroupRejectsInvalidSpendRange() {
        CustomerGroupRequest request = request("AUTOMATIC", CriteriaRuleDTO.builder()
                .minSpend(BigDecimal.valueOf(100)).maxSpend(BigDecimal.valueOf(50)).build());

        assertThrows(IllegalArgumentException.class, () -> groupService.createGroup(request));
        verify(groupRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recalculationAssignsCustomerOnlyToMoreSpecificMatchingGroup() {
        ObjectMapper mapper = new ObjectMapper();
        CustomerGroup potential = automaticGroup(mapper, 1L, "Tiềm năng", 1);
        CustomerGroup vip = automaticGroup(mapper, 2L, "VIP", 3);
        CustomerGroup manualGroup = new CustomerGroup();
        manualGroup.setId(3L);
        manualGroup.setName("Nhóm thủ công");
        manualGroup.setGroupType("MANUAL");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(potential));
        when(groupRepository.findAllByDelFlagFalseOrderByNameAsc()).thenReturn(List.of(potential, vip, manualGroup));
        when(memberRepository.findAllByDelFlagFalseAndIsManualTrueAndIsExcludedFalseOrderByAddedAtDescIdDesc()).thenReturn(List.of());
        when(memberRepository.findAllByGroupIdInAndDelFlagFalseAndIsManualTrueAndIsExcludedTrue(List.of(1L, 2L))).thenReturn(List.of());
        when(analyticsRepository.matchingCustomerIds(any(), any(), eq(1), any(), any(), any())).thenReturn(List.of(99L, 100L));
        when(analyticsRepository.matchingCustomerIds(any(), any(), eq(3), any(), any(), any())).thenReturn(List.of(99L));

        CustomerGroupService service = serviceWith(mapper);
        int result = service.evaluateAutomaticGroup(1L);

        assertEquals(1, result, "Only customer #100 should remain in the broader potential group");
        ArgumentCaptor<CustomerGroupMember> saved = ArgumentCaptor.forClass(CustomerGroupMember.class);
        verify(memberRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertEquals(java.util.Set.of(100L, 99L), saved.getAllValues().stream().map(CustomerGroupMember::getCustomerId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of(1L, 2L), saved.getAllValues().stream().map(CustomerGroupMember::getGroupId).collect(java.util.stream.Collectors.toSet()));
        verify(memberRepository).softDeleteAutomaticMembers(1L);
        verify(memberRepository).softDeleteAutomaticMembers(2L);
    }

    @Test
    void manualExclusionFromVipFallsBackToOtherMatchingGroup() {
        ObjectMapper mapper = new ObjectMapper();
        CustomerGroup potential = automaticGroup(mapper, 1L, "Tiềm năng", 1);
        CustomerGroup vip = automaticGroup(mapper, 2L, "VIP", 3);
        CustomerGroupMember exclusion = new CustomerGroupMember();
        exclusion.setGroupId(2L);
        exclusion.setCustomerId(99L);
        exclusion.setIsManual(true);
        exclusion.setIsExcluded(true);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(potential));
        when(groupRepository.findAllByDelFlagFalseOrderByNameAsc()).thenReturn(List.of(potential, vip));
        when(memberRepository.findAllByDelFlagFalseAndIsManualTrueAndIsExcludedFalseOrderByAddedAtDescIdDesc()).thenReturn(List.of());
        when(memberRepository.findAllByGroupIdInAndDelFlagFalseAndIsManualTrueAndIsExcludedTrue(List.of(1L, 2L))).thenReturn(List.of(exclusion));
        when(analyticsRepository.matchingCustomerIds(any(), any(), any(), any(), any(), any())).thenReturn(List.of(99L));

        serviceWith(mapper).evaluateAutomaticGroup(1L);

        ArgumentCaptor<CustomerGroupMember> saved = ArgumentCaptor.forClass(CustomerGroupMember.class);
        verify(memberRepository).save(saved.capture());
        assertEquals(1L, saved.getValue().getGroupId());
    }

    @Test
    void manualAssignmentMovesWhenCustomerQualifiesForAutomaticGroup() {
        ObjectMapper mapper = new ObjectMapper();
        CustomerGroup potential = automaticGroup(mapper, 1L, "Tiềm năng", 1);
        CustomerGroup vip = automaticGroup(mapper, 2L, "VIP", 3);
        CustomerGroup manualGroup = new CustomerGroup();
        manualGroup.setId(3L);
        manualGroup.setName("Nhóm thủ công");
        manualGroup.setGroupType("MANUAL");
        CustomerGroupMember manualAssignment = new CustomerGroupMember();
        manualAssignment.setGroupId(3L);
        manualAssignment.setCustomerId(99L);
        manualAssignment.setIsManual(true);
        manualAssignment.setIsExcluded(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(potential));
        when(groupRepository.findAllByDelFlagFalseOrderByNameAsc()).thenReturn(List.of(potential, vip, manualGroup));
        manualAssignment.setId(55L);
        when(memberRepository.findAllByDelFlagFalseAndIsManualTrueAndIsExcludedFalseOrderByAddedAtDescIdDesc()).thenReturn(List.of(manualAssignment));
        when(memberRepository.findAllByGroupIdInAndDelFlagFalseAndIsManualTrueAndIsExcludedTrue(List.of(1L, 2L))).thenReturn(List.of());
        when(analyticsRepository.matchingCustomerIds(any(), any(), any(), any(), any(), any())).thenReturn(List.of(99L));

        serviceWith(mapper).evaluateAutomaticGroup(1L);

        verify(memberRepository).softDeleteManualMembershipsForCustomer(99L);
        ArgumentCaptor<CustomerGroupMember> saved = ArgumentCaptor.forClass(CustomerGroupMember.class);
        verify(memberRepository).save(saved.capture());
        assertEquals(1L, saved.getValue().getGroupId());
        assertEquals(99L, saved.getValue().getCustomerId());
        verify(memberRepository).softDeleteAutomaticMembers(1L);
        verify(memberRepository).softDeleteAutomaticMembers(2L);
    }

    private CustomerGroupService serviceWith(ObjectMapper mapper) {
        return new CustomerGroupService(groupRepository, memberRepository, customerRepository, analyticsRepository, mapper);
    }

    @Test
    void addingCustomerToNewGroupMovesThemOutOfPreviousGroups() {
        CustomerGroup target = new CustomerGroup();
        target.setId(2L);
        Customer customer = new Customer();
        customer.setId(99L);
        when(groupRepository.findById(2L)).thenReturn(Optional.of(target));
        when(customerRepository.findById(99L)).thenReturn(Optional.of(customer));
        when(memberRepository.save(any(CustomerGroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerGroupMember moved = serviceWith(new ObjectMapper()).addMember(2L, 99L);

        verify(memberRepository).softDeleteAllMembershipsForCustomer(99L);
        assertEquals(2L, moved.getGroupId());
        assertEquals(99L, moved.getCustomerId());
        assertEquals(true, moved.getIsManual());
        assertEquals(false, moved.getIsExcluded());
    }

    private CustomerGroup automaticGroup(ObjectMapper mapper, long id, String name, int minOrders) {
        CustomerGroup group = new CustomerGroup();
        group.setId(id);
        group.setName(name);
        group.setGroupType("AUTOMATIC");
        group.setCriteriaRule(mapper.createObjectNode().put("minOrders", minOrders));
        return group;
    }

    private CustomerGroupRequest request(String groupType, CriteriaRuleDTO rule) {
        CustomerGroupRequest request = new CustomerGroupRequest();
        request.setName("Nhóm kiểm thử");
        request.setGroupType(groupType);
        request.setCriteriaRule(rule);
        return request;
    }
}
