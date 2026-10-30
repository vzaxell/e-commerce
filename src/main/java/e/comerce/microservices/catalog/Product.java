package e.comerce.microservices.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;
    private String name;
    @Column(length = 1000)
    private String description;
    private String category;
    @Column(name = "price_usd", precision = 12, scale = 2)
    private BigDecimal priceUsd;

    protected Product() {}

    public Product(String id, String name, String description, String category, BigDecimal priceUsd) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.priceUsd = priceUsd;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPriceUsd() { return priceUsd; }
}
