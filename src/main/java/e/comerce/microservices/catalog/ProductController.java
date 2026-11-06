package e.comerce.microservices.catalog;

import e.comerce.microservices.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/microservices/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProductDto> findAll() {
        return repository.findAll().stream()
                .map(ProductDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@RequestBody ProductDto dto) {
        Product saved = repository.save(
                new Product(dto.id(), dto.name(), dto.description(), dto.category(), dto.priceUsd()));
        return ProductDto.from(saved);
    }
}
