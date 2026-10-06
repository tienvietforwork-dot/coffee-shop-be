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
        boolean first = addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customer.getId()).isEmpty();
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
