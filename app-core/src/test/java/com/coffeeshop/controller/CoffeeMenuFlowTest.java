package com.coffeeshop.controller;

import com.coffeeshop.entity.Category;
import com.coffeeshop.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Thực đơn: a drink name appears once, whatever its category. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"menu.edit"})
class CoffeeMenuFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CategoryRepository categoryRepository;

    private String coffee(Long categoryId, String name) throws Exception {
        return json.writeValueAsString(Map.of("categoryId", categoryId, "name", name, "price", 39000));
    }

    @Test
    void aDrinkNameIsUniqueOnTheMenu() throws Exception {
        Long a = categoryRepository.save(Category.builder().name("A " + UUID.randomUUID()).build()).getId();
        Long b = categoryRepository.save(Category.builder().name("B " + UUID.randomUUID()).build()).getId();
        String name = "Cà phê muối " + UUID.randomUUID().toString().substring(0, 6);
        String id = json.readTree(mvc.perform(post("/api/coffees").contentType(MediaType.APPLICATION_JSON).content(coffee(a, name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/coffees").contentType(MediaType.APPLICATION_JSON).content(coffee(b, name.toUpperCase())))
                .andExpect(status().isBadRequest());
        // saving the same drink again keeps its own name
        mvc.perform(put("/api/coffees/" + id).contentType(MediaType.APPLICATION_JSON).content(coffee(b, name)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name));
    }
}
