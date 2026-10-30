package e.comerce.microservices.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InventoryDto(
        @JsonProperty("product_id") String productId,
        @JsonProperty("available_units") int availableUnits,
        @JsonProperty("warehouse_location") String warehouseLocation,
        @JsonProperty("estimated_delivery_days") int estimatedDeliveryDays) {

    public static InventoryDto from(InventoryItem i) {
        return new InventoryDto(i.getProductId(), i.getAvailableUnits(),
                i.getWarehouseLocation(), i.getEstimatedDeliveryDays());
    }
}
