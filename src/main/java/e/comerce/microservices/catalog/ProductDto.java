package e.comerce.microservices.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProductDto(
        String id,
        String name,
        String description,
        String category,
        @JsonProperty("price_usd") BigDecimal priceUsd) {

    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getPriceUsd());
    }
}
