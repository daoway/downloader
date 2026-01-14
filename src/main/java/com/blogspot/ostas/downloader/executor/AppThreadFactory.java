package com.blogspot.ostas.downloader.executor;

import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AppThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("downloader-pool-" + threadNumber.getAndIncrement());
        thread.setUncaughtExceptionHandler(new AppExceptionHandler());
        thread.setDaemon(false);
        return thread;
    }
}
