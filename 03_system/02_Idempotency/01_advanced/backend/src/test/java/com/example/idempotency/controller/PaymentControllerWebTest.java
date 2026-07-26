package com.example.idempotency.controller;

import com.example.idempotency.domain.CartStatus;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void paymentJson_canBindLegacyOrderIdAlias() throws Exception {
        when(paymentService.process(eq("idempo-1"), eq(new PaymentDto.Request("1001", "cust-001", 15_000L))))
            .thenReturn(new PaymentDto.Response("1001", "pay-1", "pg-1", CartStatus.PAID, Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(
                post("/api/payments")
                    .header("Idempotency-Key", "idempo-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"orderId":"1001","customerId":"cust-001","amount":15000}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cartId").value("1001"))
            .andExpect(jsonPath("$.status").value("PAID"));

        var requestCaptor = forClass(PaymentDto.Request.class);
        verify(paymentService).process(eq("idempo-1"), requestCaptor.capture());
        assertThat(requestCaptor.getValue().cartId()).isEqualTo("1001");
    }

    @Test
    void legacyOrderIdQuery_isAcceptedForPaymentsLookup() throws Exception {
        when(paymentService.getPayments("1001")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/payments").param("orderId", "1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        verify(paymentService).getPayments("1001");
    }
}
