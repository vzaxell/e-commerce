package e.comerce.microservices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean(destroyMethod = "")
    public Executor compositeExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
