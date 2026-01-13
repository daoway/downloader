package com.blogspot.ostas.downloader.app;

import com.blogspot.ostas.downloader.service.Downloader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "command.line.runner", value = "enabled", havingValue = "true", matchIfMissing = true)
public class DownloadAppExecutor implements CommandLineRunner {

  private final Downloader downloader;

  @Override
  public void run(String... args) {
    var url = "https://dlcdn.apache.org/spark/spark-4.1.1/spark-4.1.1-bin-hadoop3.tgz";
    downloader.download(url);
  }

}
