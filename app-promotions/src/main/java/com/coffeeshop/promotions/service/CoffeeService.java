package com.coffeeshop.promotions.service;

import com.coffeeshop.promotions.dto.response.CoffeeResponse;
import com.coffeeshop.promotions.entity.Coffee;
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
}
