package com.example.idempotency.controller;

import com.example.idempotency.domain.CartStatus;
import com.example.idempotency.dto.CartDto;
import com.example.idempotency.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Test
    void legacyOrderPath_getCart_isRoutedToCartService() throws Exception {
        when(cartService.getCart("1001"))
            .thenReturn(new CartDto.Response("1001", CartStatus.PAID, "pay-1", "pg-1"));

        mockMvc.perform(get("/api/orders/1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cartId").value("1001"))
            .andExpect(jsonPath("$.status").value("PAID"));

        verify(cartService).getCart("1001");
    }

    @Test
    void legacyOrderPath_currentCart_isRoutedToCartService() throws Exception {
        when(cartService.getCurrentCart())
            .thenReturn(new CartDto.Response("1001", CartStatus.PENDING, null, null));

        mockMvc.perform(get("/api/orders/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cartId").value("1001"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(cartService).getCurrentCart();
    }

    @Test
    void legacyOrderPath_nextCart_isRoutedToCartService() throws Exception {
        when(cartService.createNextCart())
            .thenReturn(new CartDto.Response("1002", CartStatus.PENDING, null, null));

        mockMvc.perform(post("/api/orders/next").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cartId").value("1002"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(cartService).createNextCart();
    }
}
