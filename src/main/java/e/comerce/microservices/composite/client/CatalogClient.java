package e.comerce.microservices.composite.client;

import e.comerce.microservices.catalog.ProductDto;
import e.comerce.microservices.exception.ResourceNotFoundException;
import e.comerce.microservices.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CatalogClient {

    private final RestClient client;

    public CatalogClient(@Qualifier("internalRestClient") RestClient client) {
        this.client = client;
    }

    public ProductDto getProduct(String id) {
        try {
            return client.get()
                    .uri("/microservices/products/{id}", id)
                    .retrieve()
                    .body(ProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Producto no encontrado: " + id);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Catálogo de productos no disponible");
        }
    }
}
