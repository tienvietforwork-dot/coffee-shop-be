package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.service.CustomerGroupService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class CustomerGroupControllerTest {

    @Test
    void exportUsesUtf8CsvAndEscapesCommasAndQuotes() {
        CustomerGroupService service = mock(CustomerGroupService.class);
        when(service.exportRows(7L)).thenReturn(List.<Object[]>of(new Object[]{
                4L, "Nguyễn, \"A\"", "0901234567", "a@example.com", 14, 2L, new BigDecimal("147000.00")
        }));

        var response = new CustomerGroupController(service).exportMembers(7L);
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFFMã khách hàng,Họ tên,Số điện thoại,Email,Điểm tích lũy,Số đơn hoàn tất,Tổng chi tiêu\r\n"));
        assertTrue(csv.contains("\"4\",\"Nguyễn, \"\"A\"\"\",\"0901234567\",\"a@example.com\",\"14\",\"2\",\"147000.00\"\r\n"));
        assertTrue(response.getHeaders().getContentDisposition().getFilename().contains("nhom-khach-hang-7.csv"));
    }

    @Test
    void exportEscapesSpreadsheetFormulaCells() {
        CustomerGroupService service = mock(CustomerGroupService.class);
        when(service.exportRows(7L)).thenReturn(List.<Object[]>of(new Object[]{
                4L, "  =HYPERLINK(\"https://example.invalid\")", "0901234567", "", 0, 0L, BigDecimal.ZERO
        }));

        var response = new CustomerGroupController(service).exportMembers(7L);
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"'  =HYPERLINK(\"\"https://example.invalid\"\")\""));
    }
}
