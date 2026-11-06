package e.comerce.microservices.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import e.comerce.microservices.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductController controller;

    private Product laptop;
    private Product mouse;

    @BeforeEach
    void setUp() {
        laptop = new Product("P1", "Laptop", "Laptop 14 pulgadas", "Computo", new BigDecimal("1000.00"));
        mouse = new Product("P2", "Mouse", "Mouse inalámbrico", "Accesorios", new BigDecimal("25.50"));
    }

    @Test
    @DisplayName("findAll: devuelve la lista de productos simulada por el repositorio")
    void findAll_returnsMappedList() {
        when(repository.findAll()).thenReturn(List.of(laptop, mouse));

        List<ProductDto> result = controller.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductDto::id).containsExactly("P1", "P2");
        assertThat(result.get(0).name()).isEqualTo("Laptop");
        assertThat(result.get(1).priceUsd()).isEqualByComparingTo("25.50");
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("findAll: devuelve lista vacía cuando no hay productos")
    void findAll_empty() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(controller.findAll()).isEmpty();
        verify(repository).findAll();
    }

    @Test
    @DisplayName("create: persiste el producto y verifica sus atributos")
    void create_savesProductAndReturnsDto() {
        ProductDto request = new ProductDto("P3", "Teclado", "Teclado mecánico", "Accesorios",
                new BigDecimal("80.00"));
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto result = controller.create(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repository, times(1)).save(captor.capture());
        Product persisted = captor.getValue();
        assertThat(persisted.getId()).isEqualTo("P3");
        assertThat(persisted.getName()).isEqualTo("Teclado");
        assertThat(persisted.getDescription()).isEqualTo("Teclado mecánico");
        assertThat(persisted.getCategory()).isEqualTo("Accesorios");
        assertThat(persisted.getPriceUsd()).isEqualByComparingTo("80.00");

        assertThat(result).isEqualTo(request);
    }

    @Test
    @DisplayName("getById: devuelve el DTO cuando el producto existe")
    void getById_found() {
        when(repository.findById("P1")).thenReturn(Optional.of(laptop));

        ProductDto result = controller.getById("P1");

        assertThat(result.id()).isEqualTo("P1");
        assertThat(result.name()).isEqualTo("Laptop");
        assertThat(result.description()).isEqualTo("Laptop 14 pulgadas");
        assertThat(result.category()).isEqualTo("Computo");
        assertThat(result.priceUsd()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("getById: lanza ResourceNotFoundException cuando no existe")
    void getById_notFound_throws() {
        when(repository.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getById("X"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Producto no encontrado: X");
        verify(repository, never()).save(any());
    }
}
