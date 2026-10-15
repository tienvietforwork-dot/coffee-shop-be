package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.dto.response.CustomerPortalFeedbackResponse;
import com.coffeeshop.crm.dto.response.CustomerPortalInteractionResponse;
import com.coffeeshop.crm.dto.response.CustomerPortalReadReceiptResponse;
import com.coffeeshop.crm.security.CrmPrincipal;
import com.coffeeshop.crm.service.CustomerPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-portal")
@RequiredArgsConstructor
@Tag(name = "Cổng thông tin khách hàng", description = "Khách chỉ xem phản hồi và tương tác thuộc hồ sơ đã liên kết với tài khoản")
public class CustomerPortalController {
    private final CustomerPortalService customerPortalService;

    @GetMapping("/feedbacks")
    @Operation(summary = "Khách xem góp ý của mình và nội dung nhân viên trả lời")
    public ResponseEntity<Page<CustomerPortalFeedbackResponse>> feedbacks(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return ResponseEntity.ok(customerPortalService.feedbacks(principal(authentication), page, size));
    }

    @GetMapping("/interactions")
    @Operation(summary = "Khách xem các tương tác nhân viên đã cho phép hiển thị")
    public ResponseEntity<Page<CustomerPortalInteractionResponse>> interactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return ResponseEntity.ok(customerPortalService.interactions(principal(authentication), page, size));
    }

    @PostMapping("/feedbacks/{feedbackId}/responses/{responseId}/read")
    @Operation(summary = "Khách đánh dấu câu trả lời đã đọc; chỉ ghi thời điểm đọc đầu tiên")
    public ResponseEntity<CustomerPortalReadReceiptResponse> markResponseRead(
            Authentication authentication,
            @PathVariable Long feedbackId,
            @PathVariable Long responseId) {
        return ResponseEntity.ok(customerPortalService.markResponseRead(
                principal(authentication), feedbackId, responseId));
    }

    private CrmPrincipal principal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof CrmPrincipal p ? p : null;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page phải >= 0, size phải từ 1 đến 100");
        }
    }
}
