package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.util.DownloadException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.blogspot.ostas.downloader.util.TestUtils.concurrentDownload;
import static com.blogspot.ostas.downloader.util.TestUtils.downloadFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Testcontainers
@SpringBootTest(properties = {"command.line.runner.enabled=false"})
@Slf4j
class NginxIntegrationTest {

    @Container
    private static final GenericContainer<?> nginx =
            new GenericContainer<>(
                    new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder
                            .from("alpine:latest")
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
    private Downloader downloader;

    @Test
    void serverConnectionLimit() {
        final String url =
                "http://localhost:%s/downloads/file.out".formatted(nginx.getFirstMappedPort());
        final int numberOfThreads = 3;

        List<String> downloadedFileNames = Collections.synchronizedList(new ArrayList<>());

        var errors = concurrentDownload(url, numberOfThreads, (u, index) -> {
            var file = fileService.filename(url) + "." + index;
            System.out.println(file);
            downloadFile(url, file, httpClient);
            downloadedFileNames.add(file);
        });

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst())
                .isInstanceOf(DownloadException.class)
                .hasMessage("Non OK code : 503");

        downloadedFileNames.forEach(file -> deleteIfExists(Path.of(file)));
        assertThat(downloadedFileNames).hasSize(2);
    }

    @Test
    void downloadFileWithConnectionLimit() {
        var fileName = "file.out";
        var url = "http://localhost:%s/downloads/%s"
                .formatted(nginx.getFirstMappedPort(), fileName);

        var downloadResult = downloader.download(url);

        var baseName = fileService.filename(url);
        deleteIfExists(Path.of(baseName));
        deleteChunkFiles(baseName);

        assertThat(downloadResult.getChunkErrors()).isEmpty();
    }

    @Test
    void whenHttpHeadIsDisabled() {
        var fileName = "file.out";
        var url = "http://localhost:%s/upload/%s"
                .formatted(nginx.getFirstMappedPort(), fileName);

        downloader.download(url);

        assertThat(new File(fileName)).doesNotExist();
    }

    private void deleteIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            fail("Error removing file " + file, e);
        }
    }

    private void deleteChunkFiles(String baseName) {
        try (var stream = Files.list(Path.of("."))) {
            stream
                    .filter(p -> p.getFileName().toString().startsWith(baseName + "."))
                    .peek(x -> {
                        System.out.println(x);
                    })
                    .forEach(this::deleteIfExists);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}