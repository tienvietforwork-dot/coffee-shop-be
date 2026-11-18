package com.coffeeshop.crm.service;

import com.coffeeshop.crm.repository.CustomerAnalyticsRepository;
import com.coffeeshop.crm.repository.CustomerInsightRepository;
import com.coffeeshop.crm.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerInsightServiceTest {
    @Mock CustomerInsightRepository insightRepository;
    @Mock CustomerAnalyticsRepository analyticsRepository;
    @Mock CustomerRepository customerRepository;
    @InjectMocks CustomerInsightService insightService;

    @Test
    void cohortAllowsUnboundedDateRange() {
        when(insightRepository.cohort(null, null)).thenReturn(List.of());

        insightService.cohorts(null, null);

        verify(insightRepository).cohort(null, null);
    }

    @Test
    void cohortRejectsReversedRangeBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> insightService.cohorts(
                LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 28)));

        verify(insightRepository, never()).cohort(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void boundedInsightOptionsRejectUnsupportedValues() {
        assertThrows(IllegalArgumentException.class, () -> insightService.topCustomers(null, null, 0, "spend"));
        assertThrows(IllegalArgumentException.class, () -> insightService.topCustomers(null, null, 10, "unknown"));
        assertThrows(IllegalArgumentException.class, () -> insightService.inactiveCustomers(0, 10));
        assertThrows(IllegalArgumentException.class, () -> insightService.monthlySales(null, null, "year"));
        assertThrows(IllegalArgumentException.class, () -> insightService.feedbackTrends(null, null, "quarter"));
        assertThrows(IllegalArgumentException.class, () -> insightService.productAffinity("invalid", null, null, 10));
    }
}
