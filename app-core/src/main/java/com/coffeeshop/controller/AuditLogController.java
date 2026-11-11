package com.coffeeshop.controller;

import com.coffeeshop.entity.AuditLog;
import com.coffeeshop.export.ExcelSheet;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Nhật ký thao tác: tìm theo người, chức năng, thời gian, chỉ lỗi; xuất Excel. Read-only. */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    @PreAuthorize(Perm.CAN_AUDIT_LOGS)
    public Map<String, Object> search(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(required = false) String username,
                                      @RequestParam(required = false) String module,
                                      @RequestParam(defaultValue = "false") boolean failed,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size) {
        service.flush(); // show what was done a second ago too
        Page<AuditLog> result = service.search(from, to, username, module, failed, keyword, page, size);
        return Map.of("items", result.getContent(), "total", result.getTotalElements());
    }

    @GetMapping("/filters")
    @PreAuthorize(Perm.CAN_AUDIT_LOGS)
    public Map<String, List<String>> filters() {
        return Map.of("usernames", service.usernames(), "modules", service.modules());
    }

    @GetMapping("/export.xlsx")
    @PreAuthorize(Perm.CAN_AUDIT_LOGS)
    public ResponseEntity<byte[]> excel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(required = false) String username,
                                        @RequestParam(required = false) String module,
                                        @RequestParam(defaultValue = "false") boolean failed,
                                        @RequestParam(required = false) String keyword) {
        service.flush();
        ExcelSheet x = new ExcelSheet("Nhat ky").title("NHẬT KÝ THAO TÁC")
                .line("Từ " + from + " đến " + to)
                .header("Thời gian", "Người thao tác", "Chức năng", "Thao tác", "Mã bản ghi", "Kết quả", "Đường dẫn", "IP", "Nội dung");
        for (AuditLog a : service.search(from, to, username, module, failed, keyword, 0, 500).getContent()) {
            x.row(a.getCreatedAt(), a.getUsername() == null ? "Khách" : a.getUsername(), a.getModule(), a.getAction(),
                    a.getEntityId(), a.getStatusCode() < 400 ? "Thành công" : "Lỗi " + a.getStatusCode(), a.getMethod() + " " + a.getPath(),
                    a.getIp(), a.getDetail());
        }
        return x.download("nhat-ky-thao-tac-" + from + "_" + to);
    }
}
