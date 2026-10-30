package e.comerce.microservices.inventory;

import e.comerce.microservices.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/microservices/inventory")
public class InventoryController {

    private final InventoryRepository repository;

    public InventoryController(InventoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{productId}")
    public InventoryDto getByProductId(@PathVariable String productId) {
        return repository.findById(productId)
                .map(InventoryDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado para: " + productId));
    }
}
