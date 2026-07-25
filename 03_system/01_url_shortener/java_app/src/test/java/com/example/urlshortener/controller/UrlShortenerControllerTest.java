package com.example.urlshortener.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.urlshortener.config.GlobalExceptionHandler;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.service.UrlShortenerService;
import com.example.urlshortener.util.ShortCodeConverter;

class UrlShortenerControllerTest {

    private MockMvc mockMvc;
    private UrlShortenerService urlShortenerService;
    private UrlShortenerController controller;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        conversionService.addConverter(new ShortCodeConverter());

        urlShortenerService = mock(UrlShortenerService.class);
        controller = new UrlShortenerController(urlShortenerService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setConversionService(conversionService)
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid!", "01", "AzL8n0Y58m8"})
    void shouldReturnBadRequestWhenShortCodeIsInvalid(String shortCode) throws Exception {
        mockMvc.perform(get("/api/v1/{shortUrl}", shortCode))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_SHORT_CODE"))
                .andExpect(jsonPath("$.message").value("단축 코드 형식이 올바르지 않습니다."));
    }

    @Test
    void shouldReturnApiErrorWhenRequestBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/data/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-url"})
    void shouldReturnValidationErrorWhenLongUrlIsInvalid(String longUrl) throws Exception {
        mockMvc.perform(post("/api/v1/data/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"" + longUrl + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnNotFoundWhenShortUrlDoesNotExist() throws Exception {
        when(urlShortenerService.getOriginalUrl(any()))
                .thenThrow(new UrlNotFoundException("10"));

        mockMvc.perform(get("/api/v1/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenDatastoreFails() throws Exception {
        when(urlShortenerService.getOriginalUrl(any()))
                .thenThrow(new DataAccessResourceFailureException("Mongo unavailable"));

        mockMvc.perform(get("/api/v1/10"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATASTORE_UNAVAILABLE"));
    }

    @Test
    void shouldRedirectWithFoundAndLocationHeader() throws Exception {
        when(urlShortenerService.getOriginalUrl(any()))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/api/v1/10"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void shouldRedirectPermanentlyWhenConfiguredFor301() throws Exception {
        ReflectionTestUtils.setField(controller, "redirectMode", "301");
        when(urlShortenerService.getOriginalUrl(any()))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/api/v1/10"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://example.com"));
    }
}
