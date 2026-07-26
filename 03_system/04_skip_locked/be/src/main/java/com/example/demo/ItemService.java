package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public ResponseEntity<?> buy(Long id, int quantity) {
        if (quantity != 1) {
            return ResponseEntity.badRequest()
                .body("This experiment supports quantity 1 only");
        }

        Item item = itemRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RuntimeException("Item not found"));

            if (item.getQuantity() < quantity) {
            throw new RuntimeException("Not enough quantity available");
        }

        item.setQuantity(item.getQuantity() - quantity);

        return ResponseEntity.ok("Purchase successful");
    }
}
