package com.coffeeshop.crm.service;

import com.coffeeshop.crm.repository.CustomerRepository;
import com.coffeeshop.crm.repository.InteractionReminderNotificationRepository;
import com.coffeeshop.crm.repository.InteractionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {
    @Mock InteractionRepository interactionRepository;
    @Mock InteractionReminderNotificationRepository reminderNotificationRepository;
    @Mock CustomerRepository customerRepository;
    @InjectMocks InteractionService interactionService;

    @Test
    void searchAllowsAllOptionalFiltersToBeOmitted() {
        when(interactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<?> result = interactionService.search(null, null, null, null, null, null, null, 0, 20);

        assertEquals(0, result.getTotalElements());
        verify(interactionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchAcceptsSingleDayRange() {
        when(interactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        LocalDate sameDay = LocalDate.of(2026, 10, 28);

        interactionService.search(4L, 2L, "CALL", "CLOSED", sameDay, sameDay, "  xác nhận  ", 0, 20);

        verify(interactionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchRejectsReversedDateRangeBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> interactionService.search(
                null, null, null, null, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 28), null, 0, 20));

        verify(interactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
