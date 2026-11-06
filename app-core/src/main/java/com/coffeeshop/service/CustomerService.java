package com.coffeeshop.service;

import com.coffeeshop.dto.request.DeliveryAddressRequest;
import com.coffeeshop.dto.response.CustomerLookupResponse;
import com.coffeeshop.entity.Customer;
import com.coffeeshop.entity.CustomerAddress;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CustomerAddressRepository;
import com.coffeeshop.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal customer handling needed by sales (find-or-create by phone, delivery addresses).
 * Full customer management (CRM) lives in app-crm.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;

    @Transactional(readOnly = true)
    public CustomerLookupResponse lookup(String phone) {
        Customer customer = customerRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có khách hàng với số " + phone));
        return CustomerLookupResponse.from(customer,
                addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customer.getId()));
    }

    /**
     * Sổ địa chỉ của khách đăng nhập: thêm ({@code id} null) / sửa. Địa chỉ đầu tiên, hoặc đánh dấu mặc định, thành địa chỉ
     * mặc định (bỏ cờ ở địa chỉ khác).
     */
    @Transactional
    public CustomerAddress saveMyAddress(Long customerId, Long id, String label, String recipientName, String recipientPhone,
                                         String address, boolean makeDefault) {
        if (customerId == null) throw new BadRequestException("Đăng nhập để lưu địa chỉ");
        java.util.List<CustomerAddress> mine = addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customerId);
        CustomerAddress a = id == null ? CustomerAddress.builder().customer(byId(customerId)).build() : own(customerId, id);
        a.setLabel(label == null || label.isBlank() ? null : label.trim());
        a.setRecipientName(recipientName.trim());
        a.setRecipientPhone(recipientPhone.trim());
        a.setAddress(address.trim());
        if (makeDefault || mine.isEmpty() || (id == null && mine.stream().noneMatch(CustomerAddress::isDefaultAddress))) {
            mine.stream().filter(x -> !x.getId().equals(id)).forEach(x -> x.setDefaultAddress(false));
            a.setDefaultAddress(true);
        }
        return addressRepository.save(a);
    }

    /** xóa địa chỉ của mình; xóa địa chỉ mặc định thì địa chỉ mới nhất còn lại thành mặc định */
    @Transactional
    public void deleteMyAddress(Long customerId, Long id) {
        CustomerAddress a = own(customerId, id);
        boolean wasDefault = a.isDefaultAddress();
        addressRepository.delete(a);
        if (wasDefault) {
            addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customerId).stream()
                    .filter(x -> !x.getId().equals(id)).findFirst().ifPresent(x -> x.setDefaultAddress(true));
        }
    }

    private CustomerAddress own(Long customerId, Long id) {
        CustomerAddress a = addressRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ: " + id));
        if (customerId == null || !a.getCustomer().getId().equals(customerId)) throw new BadRequestException("Địa chỉ không thuộc về bạn");
        return a;
    }

    @Transactional(readOnly = true)
    public Customer byId(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng: " + id));
    }

    /** Returns null when no phone is given (walk-in / anonymous customer). */
    @Transactional
    public Customer findOrCreate(String phone, String fullName) {
        if (phone == null || phone.isBlank()) return null;
        Customer customer = customerRepository.findByPhone(phone.trim())
                .orElseGet(() -> customerRepository.save(Customer.builder().phone(phone.trim()).build()));
        if ((customer.getFullName() == null || customer.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
            customer.setFullName(fullName.trim());
        }
        return customer;
    }

    @Transactional
    public CustomerAddress resolveAddress(Customer customer, Long addressId, DeliveryAddressRequest delivery) {
        if (customer == null) {
            throw new BadRequestException("Đơn giao hàng cần số điện thoại khách hàng");
        }
        if (addressId != null) {
            CustomerAddress address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ: " + addressId));
            if (!address.getCustomer().getId().equals(customer.getId())) {
                throw new BadRequestException("Địa chỉ không thuộc về khách hàng này");
            }
            return address;
        }
        if (delivery == null) {
            throw new BadRequestException("Vui lòng nhập địa chỉ giao hàng");
        }
        java.util.List<CustomerAddress> saved = addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customer.getId());
        // the same address again (e.g. picked from the saved ones): reuse it instead of saving a duplicate
        String addr = delivery.address().trim(), name = delivery.recipientName().trim(), phone = delivery.recipientPhone().trim();
        for (CustomerAddress a : saved) {
            if (a.getAddress().trim().equalsIgnoreCase(addr) && a.getRecipientName().trim().equalsIgnoreCase(name)
                    && a.getRecipientPhone().trim().equals(phone)) {
                return a;
            }
        }
        boolean first = saved.isEmpty();
        return addressRepository.save(CustomerAddress.builder()
                .customer(customer)
                .label(delivery.label())
                .recipientName(delivery.recipientName().trim())
                .recipientPhone(delivery.recipientPhone().trim())
                .address(delivery.address().trim())
                .defaultAddress(first)
                .build());
    }
}
