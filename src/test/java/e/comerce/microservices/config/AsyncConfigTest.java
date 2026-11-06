package e.comerce.microservices.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AsyncConfigTest {

    @Test
    void compositeExecutor_runsTasks() throws Exception {
        Executor executor = new AsyncConfig().compositeExecutor();

        try {
            String result = CompletableFuture.supplyAsync(() -> "ok", executor).get(2, TimeUnit.SECONDS);
            assertThat(result).isEqualTo("ok");
        } finally {
            ((ExecutorService) executor).close();
        }
    }
}
