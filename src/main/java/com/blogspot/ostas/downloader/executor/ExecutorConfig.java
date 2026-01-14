package com.blogspot.ostas.downloader.executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService downloaderExecutor(AppThreadFactory threadFactory) {
        int threads = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(threads, threadFactory);
    }
}
