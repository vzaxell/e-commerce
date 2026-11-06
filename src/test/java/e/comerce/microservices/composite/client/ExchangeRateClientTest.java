package e.comerce.microservices.composite.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import e.comerce.microservices.composite.client.ExchangeRateClient.RateResult;
import e.comerce.microservices.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExchangeRateClientTest {

    private static final String PRIMARY = "https://api.frankfurter.app/latest?from=USD&to=MXN";
    private static final String FALLBACK = "https://open.er-api.com/v6/latest/USD";

    private MockRestServiceServer server;
    private ExchangeRateClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ExchangeRateClient(builder.build());
    }

    @Test
    void getUsdToMxn_primaryOk() {
        server.expect(requestTo(PRIMARY))
                .andRespond(withSuccess("{\"rates\":{\"MXN\":17.5}}", MediaType.APPLICATION_JSON));

        RateResult result = client.getUsdToMxn();

        assertThat(result.rate()).isEqualByComparingTo("17.5");
        assertThat(result.source()).isEqualTo("Frankfurter External API");
        server.verify();
    }

    @Test
    void getUsdToMxn_primaryFails_usesFallback() {
        server.expect(requestTo(PRIMARY)).andRespond(withServerError());
        server.expect(requestTo(FALLBACK))
                .andRespond(withSuccess("{\"rates\":{\"MXN\":18.2}}", MediaType.APPLICATION_JSON));

        RateResult result = client.getUsdToMxn();

        assertThat(result.rate()).isEqualByComparingTo("18.2");
        assertThat(result.source()).isEqualTo("ExchangeRate-API (fallback)");
        server.verify();
    }

    @Test
    void getUsdToMxn_primaryWithoutMxn_usesFallback() {
        server.expect(requestTo(PRIMARY))
                .andRespond(withSuccess("{\"rates\":{\"EUR\":0.9}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(FALLBACK))
                .andRespond(withSuccess("{\"rates\":{\"MXN\":19}}", MediaType.APPLICATION_JSON));

        RateResult result = client.getUsdToMxn();

        assertThat(result.source()).isEqualTo("ExchangeRate-API (fallback)");
        server.verify();
    }

    @Test
    void getUsdToMxn_primaryWithoutRates_usesFallback() {
        server.expect(requestTo(PRIMARY))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(FALLBACK))
                .andRespond(withSuccess("{\"rates\":{\"MXN\":19}}", MediaType.APPLICATION_JSON));

        assertThat(client.getUsdToMxn().rate()).isEqualByComparingTo("19");
        server.verify();
    }

    @Test
    void getUsdToMxn_bothFail_throwsServiceUnavailable() {
        server.expect(requestTo(PRIMARY)).andRespond(withServerError());
        server.expect(requestTo(FALLBACK)).andRespond(withServerError());

        assertThatThrownBy(() -> client.getUsdToMxn())
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Servicios de tipo de cambio no disponibles");
        server.verify();
    }
}
