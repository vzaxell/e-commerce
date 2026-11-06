package e.comerce.microservices.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import e.comerce.microservices.exception.GlobalExceptionHandler;
import e.comerce.microservices.exception.ResourceNotFoundException;
import e.comerce.microservices.exception.ServiceUnavailableException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProductSummaryControllerTest {

    @Mock
    private ProductSummaryService service;

    @InjectMocks
    private ProductSummaryController controller;

    private MockMvc mockMvc;

    private final ProductSummaryResponse response = new ProductSummaryResponse(
            "P1", "Laptop", new BigDecimal("100.00"), new BigDecimal("1750.00"),
            "IN_STOCK", 5, "Frankfurter External API", "SUCCESS", List.of());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void summary_delegatesToService() {
        when(service.getSummary("P1")).thenReturn(response);

        ProductSummaryResponse result = controller.summary("P1");

        assertThat(result).isSameAs(response);
        verify(service).getSummary("P1");
    }

    @Test
    void route_productSummary_returns200() throws Exception {
        when(service.getSummary("P1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/product-summary/P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value("P1"))
                .andExpect(jsonPath("$.stock_status").value("IN_STOCK"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void route_productsSummary_returns200() throws Exception {
        when(service.getSummary("P1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/P1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Laptop"));
    }

    @Test
    void notFound_returns404() throws Exception {
        when(service.getSummary("X")).thenThrow(new ResourceNotFoundException("Producto no encontrado: X"));

        mockMvc.perform(get("/api/v1/product-summary/X"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto no encontrado: X"))
                .andExpect(jsonPath("$.apiStatus").value("ERROR"));
    }

    @Test
    void serviceUnavailable_returns503() throws Exception {
        when(service.getSummary("P1")).thenThrow(new ServiceUnavailableException("Catálogo de productos no disponible"));

        mockMvc.perform(get("/api/v1/product-summary/P1"))
                .andExpect(status().isServiceUnavailable());
    }
}
