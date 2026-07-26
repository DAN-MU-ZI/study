package com.example.demo;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RequiredArgsConstructor
@RestController
public class ItemController {
    private final ItemService service;

    @PostMapping("/buy")
    public ResponseEntity<?> postMethodName(@RequestBody BuyRequest request) {        
        return service.buy(request.id(), request.quantity());
    }
    
}