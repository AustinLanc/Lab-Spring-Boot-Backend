package org.example.labbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.labbackend.model.ProductName;
import org.example.labbackend.repository.ProductNameRepository;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductNameController {

    private final ProductNameRepository productNameRepository;

    public ProductNameController(ProductNameRepository productNameRepository) {
        this.productNameRepository = productNameRepository;
    }

    @GetMapping
    public List<ProductName> getAll() {
        return productNameRepository.findAll();
    }

    @GetMapping("/{code}")
    public ResponseEntity<ProductName> getByCode(@PathVariable Integer code) {
        return productNameRepository.findById(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<ProductName> searchByName(@RequestParam String name) {
        return productNameRepository.findByNameContainingIgnoreCase(name);
    }

    @PostMapping
    public ProductName create(@RequestBody ProductName productName) {
        return productNameRepository.save(productName);
    }

    @PutMapping("/{code}")
    public ResponseEntity<ProductName> update(@PathVariable Integer code, @RequestBody ProductName productName) {
        if (!productNameRepository.existsById(code)) {
            return ResponseEntity.notFound().build();
        }
        productName.setCode(code);
        return ResponseEntity.ok(productNameRepository.save(productName));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable Integer code) {
        if (!productNameRepository.existsById(code)) {
            return ResponseEntity.notFound().build();
        }
        productNameRepository.deleteById(code);
        return ResponseEntity.noContent().build();
    }

}
