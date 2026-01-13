package com.blogspot.ostas.downloader.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.blogspot.ostas.downloader.client.DownloaderHttpClient;
import com.blogspot.ostas.downloader.service.model.Chunk;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(properties = {"command.line.runner.enabled=false"})
class DownloaderTest {

    @MockitoBean
    private DownloaderHttpClient downloaderHttpClient;

    @MockitoSpyBean
    private Downloader downloader;

    @MockitoSpyBean
    private FileService fileService;

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    void downloadChunks_downloadsAllChunksAndCountsBytes() {
        // given
        var url = "http://dummy-url-used-for-testing.com/test.out";
        long contentLength = 224;
        int threadsCount = Runtime.getRuntime().availableProcessors();

        when(downloaderHttpClient.contentLength(url)).thenReturn(contentLength);

        Set<Chunk> chunks = downloader.calculateChunks(contentLength, threadsCount);

        Map<Chunk, InputStream> chunkInputStreams = new ConcurrentHashMap<>();
        Map<Chunk, OutputStream> chunkOutputStreams = new ConcurrentHashMap<>();

        int expectedContentLength = 0;
        var fileName = fileService.filename(url);

        for (var chunk : chunks) {
            byte[] chunkBytes =
                    "contents of chunk number %s%n".formatted(chunk.index())
                            .getBytes(StandardCharsets.UTF_8);

            expectedContentLength += chunkBytes.length;

            chunkInputStreams.put(chunk, new ByteArrayInputStream(chunkBytes));
            chunkOutputStreams.put(chunk, new ByteArrayOutputStream());

            when(downloaderHttpClient.inputStreamOf(chunk, url))
                    .thenReturn(chunkInputStreams.get(chunk));

            when(fileService.outputStreamFor(chunk, fileName))
                    .thenReturn(chunkOutputStreams.get(chunk));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        AtomicLong totalDownloaded = new AtomicLong(0);

        // when
        var downloadResult =
                downloader.downloadChunks(chunks, url, executor, totalDownloaded, threadsCount);

        executor.shutdown();

        // then
        assertThat(downloadResult.getTotalDownloaded()).isEqualTo(expectedContentLength);
        assertThat(downloadResult.hasErrors()).isFalse();
    }

    @Test
    void calculateDownloadRanges() {
        // given
        long contentLength = 10L;
        int threadsCount = 3;

        // when
        var chunks = downloader.calculateChunks(contentLength, threadsCount);

        // then
        assertThat(chunks)
                .containsExactly(
                        Chunk.of(0, 3, 0),
                        Chunk.of(4, 7, 1),
                        Chunk.of(8, 9, 2)
                );
    }
}
