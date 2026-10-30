package e.comerce.microservices.composite.client;

import e.comerce.microservices.exception.ServiceUnavailableException;
import e.comerce.microservices.inventory.InventoryDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InventoryClient {

    private final RestClient client;

    public InventoryClient(@Qualifier("internalRestClient") RestClient client) {
        this.client = client;
    }

    public InventoryDto getInventory(String productId) {
        try {
            return client.get()
                    .uri("/microservices/inventory/{id}", productId)
                    .retrieve()
                    .body(InventoryDto.class);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Inventario no disponible para " + productId);
        }
    }
}
