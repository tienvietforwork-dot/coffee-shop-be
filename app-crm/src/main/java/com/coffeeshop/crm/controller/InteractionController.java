package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.dto.request.InteractionRequest;
import com.coffeeshop.crm.dto.request.InteractionCloseRequest;
import com.coffeeshop.crm.dto.request.InteractionUpdateRequest;
import com.coffeeshop.crm.dto.response.InteractionResponse;
import com.coffeeshop.crm.dto.response.InteractionReminderNotificationResponse;
import com.coffeeshop.crm.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Tag(name = "Lịch sử tương tác", description = "Ghi nhận liên hệ, người phụ trách, lịch nhắc và kết quả chăm sóc")
public class InteractionController {
    private final InteractionService interactionService;

    @GetMapping
    @Operation(summary = "Danh sách lịch sử tương tác")
    public ResponseEntity<Page<InteractionResponse>> list(@RequestParam(required = false) Long customerId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return ResponseEntity.ok(interactionService.list(customerId, page, size).map(InteractionResponse::from));
    }

    @GetMapping("/reminders")
    @Operation(summary = "Danh sách lịch nhắc chăm sóc đến hạn")
    public ResponseEntity<Page<InteractionResponse>> reminders(@RequestParam(required = false) LocalDateTime until,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        validatePage(page, size);
        return ResponseEntity.ok(interactionService.dueReminders(until == null ? LocalDateTime.now() : until, page, size)
                .map(InteractionResponse::from));
    }

    @GetMapping("/reminder-notifications")
    @Operation(summary = "Danh sách thông báo nhắc chăm sóc do tác vụ nền tạo")
    public ResponseEntity<Page<InteractionReminderNotificationResponse>> reminderNotifications(
            @RequestParam(defaultValue = "true") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        validatePage(page, size);
        return ResponseEntity.ok(interactionService.reminderNotifications(unreadOnly, page, size));
    }

    @PostMapping("/reminder-notifications/{notificationId}/read")
    @Operation(summary = "Đánh dấu thông báo nhắc chăm sóc đã xem")
    public ResponseEntity<InteractionReminderNotificationResponse> markReminderRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(interactionService.markReminderRead(notificationId));
    }

    @PostMapping
    @Operation(summary = "Ghi nhận tương tác với khách hàng")
    public ResponseEntity<InteractionResponse> create(@Valid @RequestBody InteractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InteractionResponse.from(interactionService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết tương tác")
    public ResponseEntity<InteractionResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(InteractionResponse.from(interactionService.get(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tương tác")
    public ResponseEntity<InteractionResponse> update(@PathVariable Long id, @Valid @RequestBody InteractionUpdateRequest request) {
        return ResponseEntity.ok(InteractionResponse.from(interactionService.update(id, request)));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Kết thúc tương tác và ghi kết quả")
    public ResponseEntity<InteractionResponse> close(@PathVariable Long id, @Valid @RequestBody(required = false) InteractionCloseRequest request) {
        return ResponseEntity.ok(InteractionResponse.from(interactionService.close(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tương tác")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        interactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page phải >= 0, size phải từ 1 đến 100");
    }
}
