package e.comerce.microservices.inventory;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class InventoryItem {

    @Id
    @Column(name = "product_id")
    private String productId;
    @Column(name = "available_units")
    private int availableUnits;
    @Column(name = "warehouse_location")
    private String warehouseLocation;
    @Column(name = "estimated_delivery_days")
    private int estimatedDeliveryDays;

    protected InventoryItem() {}

    public InventoryItem(String productId, int availableUnits, String warehouseLocation, int estimatedDeliveryDays) {
        this.productId = productId;
        this.availableUnits = availableUnits;
        this.warehouseLocation = warehouseLocation;
        this.estimatedDeliveryDays = estimatedDeliveryDays;
    }

    public String getProductId() { return productId; }
    public int getAvailableUnits() { return availableUnits; }
    public String getWarehouseLocation() { return warehouseLocation; }
    public int getEstimatedDeliveryDays() { return estimatedDeliveryDays; }
}
