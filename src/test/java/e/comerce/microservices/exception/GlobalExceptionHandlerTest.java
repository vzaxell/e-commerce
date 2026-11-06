package e.comerce.microservices.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/microservices/products/X");

    @Test
    @DisplayName("ResourceNotFoundException -> 404")
    void notFound_returns404() {
        ResponseEntity<ApiError> response = handler.notFound(new ResourceNotFoundException("no existe"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("no existe");
        assertThat(body.path()).isEqualTo("/microservices/products/X");
        assertThat(body.apiStatus()).isEqualTo("ERROR");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("ServiceUnavailableException -> 503")
    void unavailable_returns503() {
        ResponseEntity<ApiError> response =
                handler.unavailable(new ServiceUnavailableException("servicio caído"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("servicio caído");
        assertThat(response.getBody().error()).isEqualTo("Service Unavailable");
    }

    @Test
    @DisplayName("Exception genérica -> 500 con mensaje genérico")
    void generic_returns500() {
        ResponseEntity<ApiError> response = handler.generic(new RuntimeException("detalle interno"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Error interno del servidor");
        assertThat(response.getBody().message()).doesNotContain("detalle interno");
    }
}
