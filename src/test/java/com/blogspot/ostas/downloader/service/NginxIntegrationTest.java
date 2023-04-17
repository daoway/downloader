package com.blogspot.ostas.downloader.service;

import static com.blogspot.ostas.downloader.util.TestUtils.concurrentDownload;
import static com.blogspot.ostas.downloader.util.TestUtils.downloadFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.blogspot.ostas.downloader.client.DownloaderHttpClient;
import com.blogspot.ostas.downloader.util.DownloadException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"command.line.runner.enabled=false"})
@Slf4j
class NginxIntegrationTest {

  @Container
  private static final GenericContainer<?> nginx = new GenericContainer<>(
      new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder.from("alpine:latest")
          .run("apk add --update nginx")
          .cmd("nginx", "-g", "daemon off;")
          .build()))
      .withClasspathResourceMapping("nginx.conf", "/etc/nginx/nginx.conf", BindMode.READ_ONLY)
      .withFileSystemBind("./src/test/resources/public", "/var/www/html", BindMode.READ_ONLY)
      .withExposedPorts(9999);

  @Autowired
  private HttpClient httpClient;

  @Autowired
  private FileService fileService;

  @Autowired
  private DownloaderHttpClient downloaderHttpClient;

  @Autowired
  private Downloader downloader;

  @AfterAll
  static void cleanup() {
    nginx.stop();
  }

  @Test
  void serverConnectionLimit() {
    final String url =
        "http://localhost:%s/downloads/file.out".formatted(nginx.getFirstMappedPort());
    final int numberOfThreads = 3;
    downloaderHttpClient.setUrl(url);
    List<String> downloadedFileNames = Collections.synchronizedList(new ArrayList<>());

    var errors = concurrentDownload(url, numberOfThreads, (u, index) -> {
      var file = fileService.filename(url) + "_" + index;
      downloadFile(url, file, httpClient);
      synchronized (downloadedFileNames) {
        downloadedFileNames.add(file);
      }
    });
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0)).isInstanceOf(DownloadException.class).hasMessage("Non OK code : 503");
    assertThat(downloadedFileNames).hasSize(2);
    downloadedFileNames.forEach(file -> {
      try {
        Files.delete(Path.of(file));
      } catch (IOException exception) {
        fail("No file for removal");
      }
    });
  }

  @Test
  void downloadFileWithConnectionLimit() {
    var fileName = "file.out";
    var url = "http://localhost:%s/downloads/%s".formatted(nginx.getFirstMappedPort(), fileName);
    var downloadResult = downloader.download(url);
    var configuredServerConcurrentConnections = 2;
    assertThat(downloadResult.getChunkErrors()).hasSize(
        downloader.getChunks().size() - configuredServerConcurrentConnections);
    //cleanup
    downloadResult.getChunkErrors().forEach(((chunk, throwable) -> {
      var file = fileService.filename(url) + "." + chunk.getIndex();
      try {
        Files.deleteIfExists(Path.of(file));
      } catch (IOException exception) {
        log.error("Error :", exception);
        fail("Error removing partial downloads");
      }
    }));
  }
}
