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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"command.line.runner.enabled=false",
    "spring.main.web-application-type=servlet"})
@RequiredArgsConstructor
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntegrationTest {

  @Autowired
  private Downloader downloader;

  @LocalServerPort
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

  @Test
  void download_non_existent_file() {
    var url = "http://localhost:%s/downloads/no-such-file-exists.txt".formatted(port);
    downloader.download(url);
    assertThat(new File("no-such-file-exists.txt").exists()).isFalse();
  }
}
