package e.comerce.microservices.composite.client;

import e.comerce.microservices.composite.ExchangeRateResponse;
import e.comerce.microservices.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class ExchangeRateClient {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateClient.class);

    public record RateResult(BigDecimal rate, String source) {}

    private static final String PRIMARY = "https://api.frankfurter.app/latest?from=USD&to=MXN";
    private static final String FALLBACK = "https://open.er-api.com/v6/latest/USD";

    private final RestClient client;

    public ExchangeRateClient(@Qualifier("externalRestClient") RestClient client) {
        this.client = client;
    }

    public RateResult getUsdToMxn() {
        try {
            return new RateResult(fetch(PRIMARY), "Frankfurter External API");
        } catch (Exception e) {
            log.warn("Frankfurter falló ({}), usando respaldo ExchangeRate-API", e.getMessage());
        }
        try {
            return new RateResult(fetch(FALLBACK), "ExchangeRate-API (fallback)");
        } catch (Exception e) {
            throw new ServiceUnavailableException("Servicios de tipo de cambio no disponibles");
        }
    }

    private BigDecimal fetch(String url) {
        ExchangeRateResponse body = client.get().uri(url).retrieve().body(ExchangeRateResponse.class);
        if (body == null || body.rates() == null || body.rates().get("MXN") == null) {
            throw new IllegalStateException("Respuesta sin tasa MXN");
        }
        return body.rates().get("MXN");
    }
}
