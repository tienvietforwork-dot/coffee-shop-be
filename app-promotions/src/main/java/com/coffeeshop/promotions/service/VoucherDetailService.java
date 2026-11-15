package com.coffeeshop.promotions.service;

import com.coffeeshop.promotions.entity.VoucherDetail;
import com.coffeeshop.promotions.entity.VoucherStat;
import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import com.coffeeshop.promotions.repository.VoucherDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherDetailService {

    private final VoucherDetailRepository voucherDetailRepository;

    @Scheduled(cron = "0 */5 * * * * ")
    @Transactional
    public void autoEndExpiredVoucher() {
        LocalDateTime now = LocalDateTime.now();

        List<VoucherDetail> issuedVoucher = voucherDetailRepository.findByStatus(VoucherStatus.ISSUED);

        for (VoucherDetail detail : issuedVoucher) {
            if (detail.getExpiresAt() != null && now.isAfter(detail.getExpiresAt())) {
                detail.setStatus(VoucherStatus.EXPIRED);
                voucherDetailRepository.save(detail);
            }
        }
    }

    @Transactional
    public void revokeVoucher(Long id) {
        VoucherDetail voucherDetail = findByIdVoucherDetail(id);
        if (voucherDetail.getStatus() != VoucherStatus.ISSUED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể thu hồi voucher chưa sử dụng");
        }
        voucherDetail.setStatus(VoucherStatus.REVOKED);
        voucherDetailRepository.save(voucherDetail);
    }

    private VoucherDetail findByIdVoucherDetail(Long id) {
        return voucherDetailRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher detail"));
    }
}
