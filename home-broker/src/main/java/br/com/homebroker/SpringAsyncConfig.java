package br.com.homebroker;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class SpringAsyncConfig {
    
    @Bean(name = "proccessBook")
    public Executor proccessBook() {
        return new ThreadPoolTaskExecutor();
    }
    
    @Bean(name = "proccessExecutionReport")
    public Executor proccessExecutionReport() {
        return new ThreadPoolTaskExecutor();
    }
}
