package com.coffeeshop.service;

import com.coffeeshop.dto.request.StaffRequest;
import com.coffeeshop.dto.response.StaffResponse;
import com.coffeeshop.entity.Staff;
import com.coffeeshop.entity.enums.WorkStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.StaffRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public List<StaffResponse> findAll() {
        return staffRepository.findAllByOrderByFullNameAsc().stream().map(StaffResponse::from).toList();
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        if (staffRepository.existsByPhone(request.phone())) {
            throw new BadRequestException("Số điện thoại đã được dùng: " + request.phone());
        }
        Staff staff = Staff.builder().build();
        apply(staff, request);
        return StaffResponse.from(staffRepository.save(staff));
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = get(id);
        if (!staff.getPhone().equals(request.phone()) && staffRepository.existsByPhone(request.phone())) {
            throw new BadRequestException("Số điện thoại đã được dùng: " + request.phone());
        }
        apply(staff, request);
        return StaffResponse.from(staff);
    }

    /** Staff referenced by orders / stock history are kept; mark them RESIGNED instead of deleting. */
    @Transactional
    public void delete(Long id) {
        get(id).setWorkStatus(WorkStatus.RESIGNED);
    }

    /** Employee record of the logged-in account – recorded on orders, stock movements, incidents (null if none). */
    Staff current() {
        Long id = CurrentUser.staffId();
        return id == null ? null : staffRepository.findById(id).orElse(null);
    }

    Staff get(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên: " + id));
    }

    private void apply(Staff staff, StaffRequest request) {
        staff.setFullName(request.fullName().trim());
        staff.setPosition(request.position());
        staff.setPhone(request.phone().trim());
        staff.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
        staff.setHireDate(request.hireDate());
        if (request.workStatus() != null) staff.setWorkStatus(request.workStatus());
    }
}
