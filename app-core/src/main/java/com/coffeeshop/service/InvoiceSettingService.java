package com.coffeeshop.service;

import com.coffeeshop.entity.DeliverySetting;
import com.coffeeshop.entity.InvoiceSetting;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.integration.einvoice.EinvoiceProperties;
import com.coffeeshop.repository.DeliverySettingRepository;
import com.coffeeshop.repository.InvoiceRepository;
import com.coffeeshop.repository.InvoiceSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Thiết lập hóa đơn điện tử: người bán (tên, MST, địa chỉ — mặc định là địa chỉ quán), loại hóa đơn (thuế suất) và
 * ký hiệu. Ký hiệu hóa đơn từ máy tính tiền tự ghép theo năm: {mẫu số}C{YY}M{2 chữ tự đặt}, e.g. 2C26MCH — sang năm
 * mới là ký hiệu mới, số hóa đơn chạy lại từ 1.
 */
@Service
@RequiredArgsConstructor
public class InvoiceSettingService {

    private final InvoiceSettingRepository repository;
    private final DeliverySettingRepository deliverySettingRepository;
    private final InvoiceRepository invoiceRepository;
    private final EinvoiceProperties props;

    /**
     * {@code sellerAddress}: what was typed (null = địa chỉ quán); {@code shopAddress}: địa chỉ quán hiện tại;
     * {@code series} / {@code lastNo}: ký hiệu đang dùng năm nay và số cuối đã cấp (null = chưa có).
     */
    public record Settings(String sellerName, String sellerTaxCode, String sellerAddress, String shopAddress,
                           BigDecimal vatRate, String seriesSuffix, String taxOfficeCode, String lookupUrl,
                           String templateCode, String series, String lastNo, boolean simulated) {
    }

    public record Request(String sellerName, String sellerTaxCode, String sellerAddress, BigDecimal vatRate,
                          String seriesSuffix, String taxOfficeCode, String lookupUrl) {
    }

    /** what an invoice copies when it is made */
    public record Seller(String name, String taxCode, String address, BigDecimal vatRate, String templateCode,
                         String series, String taxOfficeCode, String lookupUrl) {
    }

    @Transactional(readOnly = true)
    public Settings settings() {
        InvoiceSetting s = setting();
        Seller seller = seller(s, LocalDate.now(DeliveryFeeService.SHOP_ZONE));
        Integer last = invoiceRepository.maxNumber(seller.series());
        return new Settings(s.getSellerName(), s.getSellerTaxCode(), s.getSellerAddress(), shopAddress(), s.getVatRate(),
                s.getSeriesSuffix(), s.getTaxOfficeCode(), s.getLookupUrl(), seller.templateCode(), seller.series(),
                last == null ? null : String.format("%08d", last), props.simulated());
    }

    @Transactional
    public Settings save(Request r) {
        String name = trim(r.sellerName());
        String taxCode = trim(r.sellerTaxCode());
        String suffix = trim(r.seriesSuffix());
        String office = trim(r.taxOfficeCode());
        if (name == null) throw new BadRequestException("Nhập tên người bán");
        if (taxCode == null || !taxCode.matches("^\\d{10}(-\\d{3})?$|^\\d{12}$")) {
            throw new BadRequestException("Mã số thuế gồm 10 số, 10 số-3 số (chi nhánh) hoặc 12 số (CCCD hộ kinh doanh)");
        }
        if (suffix == null || !suffix.toUpperCase().matches("^[A-Z]{2}$")) {
            throw new BadRequestException("Ký hiệu tự đặt gồm 2 chữ cái in hoa, e.g. CH");
        }
        if (office != null && !office.toUpperCase().matches("^[A-Z0-9]{5}$")) {
            throw new BadRequestException("Mã CQT cấp gồm 5 ký tự chữ / số");
        }
        BigDecimal vat = r.vatRate();
        if (vat != null && (vat.signum() < 0 || vat.compareTo(BigDecimal.TEN) > 0)) {
            throw new BadRequestException("Thuế suất GTGT từ 0 đến 10%");
        }
        InvoiceSetting s = setting();
        s.setSellerName(name);
        s.setSellerTaxCode(taxCode);
        s.setSellerAddress(trim(r.sellerAddress()));
        s.setVatRate(vat);
        s.setSeriesSuffix(suffix.toUpperCase());
        s.setTaxOfficeCode(office == null ? null : office.toUpperCase());
        s.setLookupUrl(trim(r.lookupUrl()));
        repository.save(s);
        return settings();
    }

    /** the seller block and ký hiệu for an invoice made on {@code day} */
    @Transactional(readOnly = true)
    public Seller seller(LocalDate day) {
        return seller(setting(), day);
    }

    private Seller seller(InvoiceSetting s, LocalDate day) {
        String template = s.getVatRate() == null ? "2" : "1";
        String series = template + "C" + String.format("%02d", day.getYear() % 100) + "M" + s.getSeriesSuffix();
        String address = s.getSellerAddress() != null ? s.getSellerAddress() : shopAddress();
        return new Seller(s.getSellerName(), s.getSellerTaxCode(), address, s.getVatRate(), template, series,
                s.getTaxOfficeCode(), s.getLookupUrl() != null ? s.getLookupUrl() : props.getLookupUrl());
    }

    private String shopAddress() {
        return deliverySettingRepository.findById(DeliverySetting.ID).map(DeliverySetting::getShopAddress)
                .orElse(props.getSellerAddress());
    }

    /** the single row; defaults from app.einvoice.* (saved on the first save) when missing */
    private InvoiceSetting setting() {
        return repository.findById(InvoiceSetting.ID).orElseGet(() -> {
            String series = props.getSeries();
            String suffix = series != null && series.length() >= 7 ? series.substring(5, 7) : "CH";
            return InvoiceSetting.builder().id(InvoiceSetting.ID).sellerName(props.getSellerName())
                    .sellerTaxCode(props.getSellerTaxCode()).vatRate(props.getVatRate()).seriesSuffix(suffix)
                    .lookupUrl(props.getLookupUrl()).build();
        });
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
