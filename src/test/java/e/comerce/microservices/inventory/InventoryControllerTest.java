package e.comerce.microservices.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import e.comerce.microservices.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryRepository repository;

    @InjectMocks
    private InventoryController controller;

    @Test
    @DisplayName("getByProductId: devuelve el inventario cuando existe")
    void getByProductId_found() {
        InventoryItem item = new InventoryItem("P1", 15, "Chihuahua", 2);
        when(repository.findById("P1")).thenReturn(Optional.of(item));

        InventoryDto result = controller.getByProductId("P1");

        assertThat(result.productId()).isEqualTo("P1");
        assertThat(result.availableUnits()).isEqualTo(15);
        assertThat(result.warehouseLocation()).isEqualTo("Chihuahua");
        assertThat(result.estimatedDeliveryDays()).isEqualTo(2);
        verify(repository).findById("P1");
    }

    @Test
    @DisplayName("getByProductId: lanza ResourceNotFoundException cuando no existe")
    void getByProductId_notFound_throws() {
        when(repository.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getByProductId("X"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Inventario no encontrado para: X");
    }
}
