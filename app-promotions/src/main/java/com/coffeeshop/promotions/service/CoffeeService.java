package com.coffeeshop.promotions.service;

import com.coffeeshop.promotions.dto.request.CoffeeRequest;
import com.coffeeshop.promotions.dto.response.CoffeeResponse;
import com.coffeeshop.promotions.entity.Coffee;
import com.coffeeshop.promotions.entity.enums.CoffeeStatus;
import com.coffeeshop.promotions.repository.CoffeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoffeeService {

    private final CoffeeRepository coffeeRepository;

    @Transactional(readOnly = true)
    public List<CoffeeResponse> getAll(){
        return coffeeRepository.findAll().stream().map(CoffeeResponse::from).toList();
    }

    @Transactional
    public CoffeeResponse create(CoffeeRequest request){

        CoffeeStatus defaultStatus = request.getStatus() != null ? request.getStatus() : CoffeeStatus.AVAILABLE;
        boolean autoSoldOut = Boolean.TRUE.equals(request.getAutoSoldOut());
        Coffee coffee = Coffee.builder()
                .category(request.getCategory())
                .name(request.getName())
                .imageUrl(request.getImageUrl())
                .price(request.getPrice())
                .sale(request.getSale())
                .salePrice(request.getSalePrice())
                .description(request.getDescription())
                .status(defaultStatus)
                .autoSoldOut(autoSoldOut)
                .build();

        Coffee saved = coffeeRepository.save(coffee);
        return CoffeeResponse.from(saved);
    }
}
