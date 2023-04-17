package com.blogspot.ostas.downloader.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(properties = {
    "command.line.runner.enabled=false"}, webEnvironment = WebEnvironment.RANDOM_PORT)
@RequiredArgsConstructor
class IntegrationTest {

  @Autowired
  private Downloader downloader;

  @Value(value = "${local.server.port}")
  private int port;

  @Test
  void downloadLocalFile() {
    var fileName = "file.out";
    var path = "./src/test/resources/public/downloads/%s".formatted(fileName);
    var expectedBytes = new File(path).length();
    var url = "http://localhost:%s/downloads/%s".formatted(port, fileName);
    var downloadResult = downloader.download(url);
    assertThat(downloadResult.getTotalDownloaded()).isEqualTo(expectedBytes);
    try {
      Files.delete(Path.of(fileName));
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

}
