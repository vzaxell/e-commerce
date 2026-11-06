package e.comerce.microservices.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import e.comerce.microservices.catalog.ProductDto;
import e.comerce.microservices.composite.client.CatalogClient;
import e.comerce.microservices.composite.client.ExchangeRateClient;
import e.comerce.microservices.composite.client.ExchangeRateClient.RateResult;
import e.comerce.microservices.composite.client.InventoryClient;
import e.comerce.microservices.exception.ResourceNotFoundException;
import e.comerce.microservices.exception.ServiceUnavailableException;
import e.comerce.microservices.inventory.InventoryDto;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductSummaryServiceTest {

    @Mock
    private CatalogClient catalogClient;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private ExchangeRateClient exchangeRateClient;

    private ProductSummaryService service;

    private final ProductDto product =
            new ProductDto("P1", "Laptop", "desc", "Computo", new BigDecimal("100.00"));

    @BeforeEach
    void setUp() {
        // Executor síncrono: las pruebas son deterministas (sin hilos reales)
        service = new ProductSummaryService(catalogClient, inventoryClient, exchangeRateClient, Runnable::run);
    }

    @Test
    @DisplayName("Todo disponible -> SUCCESS con precio en MXN")
    void getSummary_success() {
        when(catalogClient.getProduct("P1")).thenReturn(product);
        when(inventoryClient.getInventory("P1")).thenReturn(new InventoryDto("P1", 5, "Chihuahua", 2));
        when(exchangeRateClient.getUsdToMxn())
                .thenReturn(new RateResult(new BigDecimal("17.5"), "Frankfurter External API"));

        ProductSummaryResponse r = service.getSummary("P1");

        assertThat(r.productId()).isEqualTo("P1");
        assertThat(r.title()).isEqualTo("Laptop");
        assertThat(r.priceUsd()).isEqualByComparingTo("100.00");
        assertThat(r.priceMxn()).isEqualByComparingTo("1750.00");
        assertThat(r.stockStatus()).isEqualTo("IN_STOCK");
        assertThat(r.availableUnits()).isEqualTo(5);
        assertThat(r.currencyConversionSource()).isEqualTo("Frankfurter External API");
        assertThat(r.status()).isEqualTo("SUCCESS");
        assertThat(r.warnings()).isEmpty();
        verify(catalogClient).getProduct("P1");
        verify(inventoryClient).getInventory("P1");
    }

    @Test
    @DisplayName("Inventario con 0 unidades -> OUT_OF_STOCK")
    void getSummary_outOfStock() {
        when(catalogClient.getProduct("P1")).thenReturn(product);
        when(inventoryClient.getInventory("P1")).thenReturn(new InventoryDto("P1", 0, "Chihuahua", 5));
        when(exchangeRateClient.getUsdToMxn())
                .thenReturn(new RateResult(new BigDecimal("17.5"), "Frankfurter External API"));

        ProductSummaryResponse r = service.getSummary("P1");

        assertThat(r.stockStatus()).isEqualTo("OUT_OF_STOCK");
        assertThat(r.availableUnits()).isZero();
        assertThat(r.status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Falla inventario -> PARTIAL_SUCCESS con stock UNKNOWN")
    void getSummary_inventoryFails_partialSuccess() {
        when(catalogClient.getProduct("P1")).thenReturn(product);
        when(inventoryClient.getInventory("P1")).thenThrow(new ServiceUnavailableException("caído"));
        when(exchangeRateClient.getUsdToMxn())
                .thenReturn(new RateResult(new BigDecimal("17.5"), "Frankfurter External API"));

        ProductSummaryResponse r = service.getSummary("P1");

        assertThat(r.stockStatus()).isEqualTo("UNKNOWN");
        assertThat(r.availableUnits()).isNull();
        assertThat(r.priceMxn()).isEqualByComparingTo("1750.00");
        assertThat(r.status()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(r.warnings()).containsExactly("Inventario no disponible");
    }

    @Test
    @DisplayName("Inventario nulo se trata como no disponible")
    void getSummary_inventoryNull_partialSuccess() {
        when(catalogClient.getProduct("P1")).thenReturn(product);
        when(inventoryClient.getInventory("P1")).thenReturn(null);
        when(exchangeRateClient.getUsdToMxn())
                .thenReturn(new RateResult(new BigDecimal("17.5"), "Frankfurter External API"));

        ProductSummaryResponse r = service.getSummary("P1");

        assertThat(r.stockStatus()).isEqualTo("UNKNOWN");
        assertThat(r.warnings()).containsExactly("Inventario no disponible");
    }

    @Test
    @DisplayName("Falla tipo de cambio -> PARTIAL_SUCCESS sin precio MXN")
    void getSummary_rateFails_partialSuccess() {
        when(catalogClient.getProduct("P1")).thenReturn(product);
        when(inventoryClient.getInventory("P1")).thenReturn(new InventoryDto("P1", 5, "Chihuahua", 2));
        when(exchangeRateClient.getUsdToMxn()).thenThrow(new ServiceUnavailableException("caído"));

        ProductSummaryResponse r = service.getSummary("P1");

        assertThat(r.priceMxn()).isNull();
        assertThat(r.currencyConversionSource()).isEqualTo("N/A");
        assertThat(r.stockStatus()).isEqualTo("IN_STOCK");
        assertThat(r.status()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(r.warnings()).containsExactly("Tipo de cambio no disponible");
    }

    @Test
    @DisplayName("Tipo de cambio nulo se trata como no disponible")
    void getSummary_rateNull_partialSuccess() {
        when(catalogClient.getProduct("P1")).thenReturn(product);
        when(inventoryClient.getInventory("P1")).thenReturn(new InventoryDto("P1", 5, "Chihuahua", 2));
        when(exchangeRateClient.getUsdToMxn()).thenReturn(null);

        ProductSummaryResponse r = service.getSummary("P1");

        assertThat(r.priceMxn()).isNull();
        assertThat(r.warnings()).containsExactly("Tipo de cambio no disponible");
    }

    @Test
    @DisplayName("Fallan inventario y tipo de cambio -> dos advertencias")
    void getSummary_bothOptionalFail_twoWarnings() {
        when(catalogClient.getProduct("P1")).thenReturn(product);
        when(inventoryClient.getInventory("P1")).thenThrow(new ServiceUnavailableException("x"));
        when(exchangeRateClient.getUsdToMxn()).thenThrow(new ServiceUnavailableException("y"));

        ProductSummaryResponse r = service.getSummary("P1");

        assertThat(r.status()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(r.warnings()).containsExactly("Inventario no disponible", "Tipo de cambio no disponible");
    }

    @Test
    @DisplayName("Producto inexistente -> propaga ResourceNotFoundException")
    void getSummary_productNotFound_propagates() {
        when(catalogClient.getProduct("X")).thenThrow(new ResourceNotFoundException("Producto no encontrado: X"));

        assertThatThrownBy(() -> service.getSummary("X"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Producto no encontrado: X");
    }

    @Test
    @DisplayName("Catálogo caído -> propaga ServiceUnavailableException")
    void getSummary_catalogUnavailable_propagates() {
        when(catalogClient.getProduct("P1")).thenThrow(new ServiceUnavailableException("Catálogo caído"));

        assertThatThrownBy(() -> service.getSummary("P1"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Catálogo caído");
    }

    @Test
    @DisplayName("Error no-Runtime en catálogo -> ServiceUnavailableException genérica")
    void getSummary_catalogUnexpectedError_wrapped() {
        when(catalogClient.getProduct("P1")).thenThrow(new AssertionError("boom"));

        assertThatThrownBy(() -> service.getSummary("P1"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Error inesperado consultando el catálogo");
    }
}
