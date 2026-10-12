package com.coffeeshop.crm.service;

import com.coffeeshop.crm.dto.request.CustomerAddressRequest;
import com.coffeeshop.crm.entity.CustomerAddress;
import com.coffeeshop.crm.exception.CrmNotFoundException;
import com.coffeeshop.crm.repository.CustomerAddressRepository;
import com.coffeeshop.crm.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<CustomerAddress> getAddresses(Long customerId) {
        requireCustomer(customerId);
        return addressRepository.findAllByCustomerIdAndDelFlagFalseOrderByIdDesc(customerId);
    }

    @Transactional
    public CustomerAddress create(Long customerId, CustomerAddressRequest request) {
        requireCustomer(customerId);
        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(customerId);
        apply(address, request);
        return addressRepository.save(address);
    }

    @Transactional
    public CustomerAddress update(Long customerId, Long addressId, CustomerAddressRequest request) {
        requireCustomer(customerId);
        CustomerAddress address = addressRepository.findById(addressId)
                .filter(a -> a.getCustomerId().equals(customerId))
                .orElseThrow(() -> new CrmNotFoundException("Không tìm thấy địa chỉ của khách hàng này"));
        apply(address, request);
        return addressRepository.save(address);
    }

    @Transactional
    public void delete(Long customerId, Long addressId) {
        requireCustomer(customerId);
        CustomerAddress address = addressRepository.findById(addressId)
                .filter(a -> a.getCustomerId().equals(customerId))
                .orElseThrow(() -> new CrmNotFoundException("Không tìm thấy địa chỉ của khách hàng này"));
        address.setDelFlag(true);
        address.setDelUser(null);
        addressRepository.save(address);
    }

    private void apply(CustomerAddress address, CustomerAddressRequest request) {
        address.setLabel(request.getLabel());
        address.setRecipientName(request.getRecipientName().trim());
        address.setRecipientPhone(request.getRecipientPhone().trim());
        address.setAddress(request.getAddress().trim());
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault) {
            if (address.getId() == null) addressRepository.clearAllDefaults(address.getCustomerId());
            else addressRepository.clearDefaultExcept(address.getCustomerId(), address.getId());
        }
        address.setIsDefault(makeDefault);
    }

    private void requireCustomer(Long id) {
        if (!customerRepository.existsById(id)) throw new CrmNotFoundException("Không tìm thấy khách hàng ID " + id);
    }
}
