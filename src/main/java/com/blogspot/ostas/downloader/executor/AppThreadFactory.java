package com.blogspot.ostas.downloader.executor;

import java.util.concurrent.ThreadFactory;

public class AppThreadFactory implements ThreadFactory {

  @Override
  public Thread newThread(Runnable r) {
    final Thread thread = new Thread(r);
    thread.setName("downloader");
    thread.setUncaughtExceptionHandler(new AppExceptionHandler());
    return thread;
  }

}