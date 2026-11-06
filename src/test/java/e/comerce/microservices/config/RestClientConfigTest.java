package e.comerce.microservices.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    private final RestClientConfig config = new RestClientConfig();

    @Test
    void internalRestClient_isCreated() {
        RestClient client = config.internalRestClient("http://localhost:8080", 1000);

        assertThat(client).isNotNull();
    }

    @Test
    void externalRestClient_isCreated() {
        RestClient client = config.externalRestClient(2000);

        assertThat(client).isNotNull();
    }
}
