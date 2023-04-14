package com.blogspot.ostas.downloader.executor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught Exception occurred on thread: " + t.getName());
        log.error("Exception message: " + e.getMessage());
    }

}
