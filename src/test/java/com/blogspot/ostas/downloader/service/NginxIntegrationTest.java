package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.client.DownloaderHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpClient;

import static com.blogspot.ostas.downloader.util.TestUtils.concurrentDownload;
import static com.blogspot.ostas.downloader.util.TestUtils.downloadFile;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {"command.line.runner.enabled=false"})
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

    @Test
    void serverConnectionLimit() {
        final String url = "http://localhost:%s/downloads/file.out".formatted(nginx.getFirstMappedPort());
        final int numberOfThreads = 3;
        downloaderHttpClient.setUrl(url);
        var errors = concurrentDownload(url, numberOfThreads,
                (u, index) -> downloadFile(url, fileService.filename(url) + "_" + index, httpClient));
        assertThat(errors).hasSize(1);
    }

    @Test
    void downloadFileWithConnectionLimit() {
        var fileName = "file.out";
        var url = "http://localhost:%s/downloads/%s".formatted(nginx.getFirstMappedPort(), fileName);
        var actualResult = downloader.download(url);
        assertThat(actualResult.getChunksErrors()).hasSize(6);
    }
}
