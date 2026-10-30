package e.comerce.microservices.composite;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ProductSummaryController {

    private final ProductSummaryService service;

    public ProductSummaryController(ProductSummaryService service) {
        this.service = service;
    }

    // Ambas rutas mencionadas en el planteamiento
    @GetMapping({"/product-summary/{id}", "/products/{id}/summary"})
    public ProductSummaryResponse summary(@PathVariable String id) {
        return service.getSummary(id);
    }
}
