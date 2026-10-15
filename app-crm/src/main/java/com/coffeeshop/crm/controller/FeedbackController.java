package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.dto.request.FeedbackRequest;
import com.coffeeshop.crm.dto.request.FeedbackCloseRequest;
import com.coffeeshop.crm.dto.request.FeedbackResponseRequest;
import com.coffeeshop.crm.dto.request.FeedbackUpdateRequest;
import com.coffeeshop.crm.dto.request.PublicFeedbackRequest;
import com.coffeeshop.crm.dto.response.FeedbackResponse;
import com.coffeeshop.crm.dto.response.FeedbackResponseDto;
import com.coffeeshop.crm.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Phản hồi khách hàng", description = "Tiếp nhận, phân loại, giao xử lý, trả lời và phân tích phản hồi")
public class FeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping("/public")
    @Operation(summary = "Khách gửi góp ý công khai")
    public ResponseEntity<FeedbackResponse> createPublic(@Valid @RequestBody PublicFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FeedbackResponse.from(feedbackService.createPublic(request)));
    }

    @GetMapping
    @Operation(summary = "Danh sách phản hồi khách hàng")
    public ResponseEntity<Page<FeedbackResponse>> list(@RequestParam(required = false) Long customerId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page phải >= 0, size phải từ 1 đến 100");
        return ResponseEntity.ok(feedbackService.list(customerId, page, size).map(FeedbackResponse::from));
    }

    @PostMapping
    @Operation(summary = "Tiếp nhận phản hồi khách hàng")
    public ResponseEntity<FeedbackResponse> create(@Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FeedbackResponse.from(feedbackService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phản hồi")
    public ResponseEntity<FeedbackResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(FeedbackResponse.from(feedbackService.get(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật và phân công xử lý phản hồi")
    public ResponseEntity<FeedbackResponse> update(@PathVariable Long id, @Valid @RequestBody FeedbackUpdateRequest request) {
        return ResponseEntity.ok(FeedbackResponse.from(feedbackService.update(id, request)));
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "Chuyển cấp xử lý phản hồi")
    public ResponseEntity<FeedbackResponse> escalate(@PathVariable Long id, @RequestParam(required = false) Long staffId) {
        FeedbackUpdateRequest request = new FeedbackUpdateRequest();
        request.setEscalated(true);
        request.setStatus("ESCALATED");
        request.setStaffId(staffId);
        return ResponseEntity.ok(FeedbackResponse.from(feedbackService.update(id, request)));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Đóng phản hồi và ghi kết quả xử lý")
    public ResponseEntity<FeedbackResponse> close(@PathVariable Long id, @Valid @RequestBody(required = false) FeedbackCloseRequest request) {
        return ResponseEntity.ok(FeedbackResponse.from(feedbackService.close(id, request)));
    }

    @PostMapping("/{id}/responses")
    @Operation(summary = "Lưu nội dung phản hồi trong CRM; endpoint này chưa gửi tin trực tiếp cho khách")
    public ResponseEntity<FeedbackResponseDto> respond(@PathVariable Long id, @Valid @RequestBody FeedbackResponseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FeedbackResponseDto.from(feedbackService.recordResponse(id, request)));
    }

    @GetMapping("/{id}/responses")
    @Operation(summary = "Lịch sử nội dung đã phản hồi khách")
    public ResponseEntity<java.util.List<FeedbackResponseDto>> responses(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getResponses(id).stream()
                .map(FeedbackResponseDto::from).toList());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phản hồi")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
