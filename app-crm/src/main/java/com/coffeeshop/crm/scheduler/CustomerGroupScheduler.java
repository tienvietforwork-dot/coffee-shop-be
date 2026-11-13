package com.coffeeshop.crm.scheduler;

import com.coffeeshop.crm.entity.CustomerGroup;
import com.coffeeshop.crm.repository.CustomerGroupRepository;
import com.coffeeshop.crm.service.CustomerGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerGroupScheduler {

    private final CustomerGroupRepository groupRepository;
    private final CustomerGroupService customerGroupService;

    // Chạy định kỳ lúc 2:00 AM mỗi ngày (Cấu hình Cron expression)
    @Scheduled(cron = "0 */5 * * * ?", zone = "Asia/Bangkok")
    public void runAutoEvaluation() {
        log.info("Bắt đầu cập nhật phân loại khách hàng");

        // Lấy tất cả các nhóm là AUTOMATIC và chưa bị xóa mềm
        try {
            customerGroupService.recalculateClassification();
        } catch (IllegalStateException missingMigration) {
            log.warn("Bỏ qua phân loại cố định: {}", missingMigration.getMessage());
        } catch (Exception e) {
            log.error("Không thể cập nhật bốn nhóm phân loại", e);
        }

        List<CustomerGroup> autoGroups = groupRepository.findAllByGroupTypeAndDelFlagFalse("AUTOMATIC").stream()
                .filter(group -> group.getClassificationCode() == null).toList();

        for (CustomerGroup group : autoGroups) {
            try {
                customerGroupService.evaluateAutomaticGroup(group.getId());
                log.info("Đã cập nhật xong nhóm: {} (ID: {})", group.getName(), group.getId());
            } catch (Exception e) {
                log.error("Lỗi khi chạy cron cho nhóm ID: {}", group.getId(), e);
            }
        }

        log.info("Hoàn tất cập nhật phân loại khách hàng");
    }
}
