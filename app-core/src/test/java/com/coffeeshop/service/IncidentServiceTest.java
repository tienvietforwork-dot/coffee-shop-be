package com.coffeeshop.service;

import com.coffeeshop.dto.request.IncidentResolveRequest;
import com.coffeeshop.entity.Customer;
import com.coffeeshop.entity.Incident;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Voucher;
import com.coffeeshop.entity.VoucherDetail;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.repository.IncidentRepository;
import com.coffeeshop.repository.VoucherDetailRepository;
import com.coffeeshop.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock IncidentRepository incidentRepository;
    @Mock VoucherRepository voucherRepository;
    @Mock VoucherDetailRepository voucherDetailRepository;
    @Mock OrderService orderService;
    @Mock StaffService staffService;

    private IncidentService incidentService;
    private final Voucher voucher = Voucher.builder().id(3L).name("Đền bù sự cố 20K")
            .discountValue(new BigDecimal("20000")).quantity(5).validDays(30).build();

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository, voucherRepository, voucherDetailRepository,
                orderService, staffService);
        Customer customer = Customer.builder().id(7L).phone("0912345678").build();
        Order order = Order.builder().id(11L).orderCode("DH011").customer(customer).build();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(Incident.builder().id(1L).order(order).build()));
        when(voucherRepository.lockById(3L)).thenReturn(Optional.of(voucher));
    }

    @Test
    void compensationHoldsOneSlotPerVoucherIssued() {
        when(voucherDetailRepository.countTakenSlots(eq(3L), any())).thenReturn(3L);
        when(voucherDetailRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = incidentService.resolve(1L, new IncidentResolveRequest("Làm lại món", 3L, 2, null));

        assertThat(response.vouchers()).hasSize(2)
                .allSatisfy(v -> assertThat(v.discountValue()).isEqualByComparingTo("20000"));
        verify(voucherDetailRepository, org.mockito.Mockito.times(2)).save(any(VoucherDetail.class));
    }

    @Test
    void compensationIsRefusedWhenNotEnoughSlotsAreLeft() {
        when(voucherDetailRepository.countTakenSlots(eq(3L), any())).thenReturn(4L);

        assertThatThrownBy(() -> incidentService.resolve(1L, new IncidentResolveRequest("Làm lại món", 3L, 2, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("chỉ còn 1 lượt");
        verify(voucherDetailRepository, never()).save(any());
    }
}
