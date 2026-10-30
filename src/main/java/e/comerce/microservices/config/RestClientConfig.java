package e.comerce.microservices.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient internalRestClient(@Value("${app.services.base-url}") String baseUrl,
                                         @Value("${app.external.timeout-ms}") int timeout) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory(timeout))
                .build();
    }

    @Bean
    public RestClient externalRestClient(@Value("${app.external.timeout-ms}") int timeout) {
        return RestClient.builder()
                .requestFactory(factory(timeout))
                .build();
    }

    private SimpleClientHttpRequestFactory factory(int timeoutMs) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeoutMs);
        f.setReadTimeout(timeoutMs);
        return f;
    }
}
