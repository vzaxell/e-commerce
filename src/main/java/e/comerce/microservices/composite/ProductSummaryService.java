package e.comerce.microservices.composite;

import e.comerce.microservices.catalog.ProductDto;
import e.comerce.microservices.composite.client.CatalogClient;
import e.comerce.microservices.composite.client.ExchangeRateClient;
import e.comerce.microservices.composite.client.ExchangeRateClient.RateResult;
import e.comerce.microservices.composite.client.InventoryClient;
import e.comerce.microservices.exception.ServiceUnavailableException;
import e.comerce.microservices.inventory.InventoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
public class ProductSummaryService {

    private static final Logger log = LoggerFactory.getLogger(ProductSummaryService.class);

    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final ExchangeRateClient exchangeRateClient;
    private final Executor executor;

    public ProductSummaryService(CatalogClient catalogClient, InventoryClient inventoryClient,
                                 ExchangeRateClient exchangeRateClient, Executor compositeExecutor) {
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.exchangeRateClient = exchangeRateClient;
        this.executor = compositeExecutor;
    }

    public ProductSummaryResponse getSummary(String id) {
        // Las 3 llamadas se lanzan de forma concurrente
        CompletableFuture<ProductDto> productF =
                CompletableFuture.supplyAsync(() -> catalogClient.getProduct(id), executor);

        CompletableFuture<Optional<InventoryDto>> inventoryF =
                CompletableFuture.supplyAsync(() -> inventoryClient.getInventory(id), executor)
                        .handle((v, ex) -> {
                            if (ex != null) { log.warn("Inventario falló: {}", ex.getMessage()); return Optional.empty(); }
                            return Optional.ofNullable(v);
                        });

        CompletableFuture<Optional<RateResult>> rateF =
                CompletableFuture.supplyAsync(exchangeRateClient::getUsdToMxn, executor)
                        .handle((v, ex) -> {
                            if (ex != null) { log.warn("Tipo de cambio falló: {}", ex.getMessage()); return Optional.empty(); }
                            return Optional.ofNullable(v);
                        });

        // El producto es obligatorio: si falla, se propaga el error (404/503)
        ProductDto product;
        try {
            product = productF.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw new ServiceUnavailableException("Error inesperado consultando el catálogo");
        }

        Optional<InventoryDto> inventory = inventoryF.join();
        Optional<RateResult> rate = rateF.join();

        List<String> warnings = new ArrayList<>();

        String stockStatus = "UNKNOWN";
        Integer units = null;
        if (inventory.isPresent()) {
            units = inventory.get().availableUnits();
            stockStatus = units > 0 ? "IN_STOCK" : "OUT_OF_STOCK";
        } else {
            warnings.add("Inventario no disponible");
        }

        BigDecimal priceMxn = null;
        String source = "N/A";
        if (rate.isPresent()) {
            priceMxn = product.priceUsd().multiply(rate.get().rate()).setScale(2, RoundingMode.HALF_UP);
            source = rate.get().source();
        } else {
            warnings.add("Tipo de cambio no disponible");
        }

        String status = warnings.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS";

        return new ProductSummaryResponse(product.id(), product.name(), product.priceUsd(), priceMxn,
                stockStatus, units, source, status, warnings);
    }
}
