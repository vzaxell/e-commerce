package e.comerce.microservices.composite.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import e.comerce.microservices.catalog.ProductDto;
import e.comerce.microservices.exception.ResourceNotFoundException;
import e.comerce.microservices.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CatalogClientTest {

    private static final String URL = "http://localhost:8080/microservices/products/P1";

    private MockRestServiceServer server;
    private CatalogClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CatalogClient(builder.build());
    }

    @Test
    void getProduct_ok() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":\"P1\",\"name\":\"Laptop\",\"description\":\"desc\","
                                + "\"category\":\"Computo\",\"price_usd\":1000.00}",
                        MediaType.APPLICATION_JSON));

        ProductDto dto = client.getProduct("P1");

        assertThat(dto.id()).isEqualTo("P1");
        assertThat(dto.name()).isEqualTo("Laptop");
        assertThat(dto.priceUsd()).isEqualByComparingTo("1000.00");
        server.verify();
    }

    @Test
    void getProduct_404_throwsResourceNotFound() {
        server.expect(requestTo(URL)).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getProduct("P1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Producto no encontrado: P1");
    }

    @Test
    void getProduct_500_throwsServiceUnavailable() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.getProduct("P1"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Catálogo de productos no disponible");
    }
}
