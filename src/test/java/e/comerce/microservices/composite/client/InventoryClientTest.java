package e.comerce.microservices.composite.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import e.comerce.microservices.exception.ServiceUnavailableException;
import e.comerce.microservices.inventory.InventoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class InventoryClientTest {

    private static final String URL = "http://localhost:8080/microservices/inventory/P1";

    private MockRestServiceServer server;
    private InventoryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new InventoryClient(builder.build());
    }

    @Test
    void getInventory_ok() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(
                        "{\"product_id\":\"P1\",\"available_units\":7,"
                                + "\"warehouse_location\":\"Chihuahua\",\"estimated_delivery_days\":3}",
                        MediaType.APPLICATION_JSON));

        InventoryDto dto = client.getInventory("P1");

        assertThat(dto.productId()).isEqualTo("P1");
        assertThat(dto.availableUnits()).isEqualTo(7);
        assertThat(dto.warehouseLocation()).isEqualTo("Chihuahua");
        assertThat(dto.estimatedDeliveryDays()).isEqualTo(3);
        server.verify();
    }

    @Test
    void getInventory_404_throwsServiceUnavailable() {
        server.expect(requestTo(URL)).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getInventory("P1"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Inventario no disponible para P1");
    }

    @Test
    void getInventory_500_throwsServiceUnavailable() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.getInventory("P1"))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
