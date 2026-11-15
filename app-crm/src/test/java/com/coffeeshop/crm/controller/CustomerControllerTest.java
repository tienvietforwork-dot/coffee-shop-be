package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.service.Customer360Service;
import com.coffeeshop.crm.service.CustomerAddressService;
import com.coffeeshop.crm.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {
    @Mock Customer360Service customer360Service;
    @Mock CustomerAddressService addressService;
    @Mock CustomerRepository customerRepository;
    @InjectMocks CustomerController customerController;

    @Test
    void importConfirmationRejectsMissingTokenAsBadRequest() {
        assertThrows(IllegalArgumentException.class, () -> customerController.confirmImport(Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> customerController.confirmImport(Map.of("token", "  ")));
    }

    @Test
    void importPreviewReadsSemicolonSeparatedVietnameseCsvWithQuotedNewline() throws Exception {
        when(customerRepository.existsByPhoneAndDelFlagFalse(anyString())).thenReturn(false);
        String csv = "full_name;phone;email;date_of_birth;gender;description\r\n"
                + "\"Nguyễn, An\";0901234567;an@example.com;1995-05-20;OTHER;\"Dòng một\nDòng hai\"\r\n";
        MockMultipartFile file = new MockMultipartFile("file", "khach-hang.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        var response = customerController.previewImport(file);

        assertEquals(1L, response.getBody().get("valid"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> preview = (List<Map<String, Object>>) response.getBody().get("preview");
        assertEquals("Nguyễn, An", preview.get(0).get("fullName"));
        assertEquals("0901234567", preview.get(0).get("phone"));
    }

    @Test
    void importPreviewReadsUtf16ExcelCsv() throws Exception {
        when(customerRepository.existsByPhoneAndDelFlagFalse(anyString())).thenReturn(false);
        byte[] text = "full_name;phone\r\nBình;0901234567\r\n".getBytes(StandardCharsets.UTF_16LE);
        byte[] csv = new byte[text.length + 2];
        csv[0] = (byte) 0xff;
        csv[1] = (byte) 0xfe;
        System.arraycopy(text, 0, csv, 2, text.length);
        MockMultipartFile file = new MockMultipartFile("file", "khach-hang.csv", "text/csv", csv);

        var response = customerController.previewImport(file);

        assertEquals(1L, response.getBody().get("valid"));
    }
}
