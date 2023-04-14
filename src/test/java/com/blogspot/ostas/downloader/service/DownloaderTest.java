package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.client.DownloaderHttpClient;
import com.blogspot.ostas.downloader.service.model.Chunk;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"command.line.runner.enabled=false"})
class DownloaderTest {

    @MockBean
    private DownloaderHttpClient downloaderHttpClient;

    @SpyBean
    private Downloader downloader;

    @SpyBean
    private FileService fileService;

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    void download() {
        // given
        var url = "http://dummy-url-used-for-testing.com/test.out";
        long contentLength = 224;
        downloader.setNumberOfThreads(Runtime.getRuntime().availableProcessors());

        // when
        when(downloaderHttpClient.getUrl()).thenReturn(url);
        when(downloaderHttpClient.contentLength()).thenReturn(contentLength);
        downloader.calculateChunks();
        var chunks = downloader.getChunks();
        Map<Chunk, InputStream> chunkInputStreams = new ConcurrentHashMap<>();
        Map<Chunk, OutputStream> chunkOutputStreams = new ConcurrentHashMap<>();
        int expectedContentLength = 0;
        for (var chunk : chunks) {
            byte[] chunkBytes = "contents of chunk number %s%n".formatted(chunk.getIndex())
                    .getBytes(StandardCharsets.UTF_8);
            expectedContentLength += chunkBytes.length;
            chunkInputStreams.put(chunk, new ByteArrayInputStream(chunkBytes));
            chunkOutputStreams.put(chunk, new ByteArrayOutputStream());
            var fileNamePart = fileService.filename(downloaderHttpClient.getUrl());
            when(downloaderHttpClient.inputStreamOf(chunk)).thenReturn(chunkInputStreams.get(chunk));
            when(fileService.outputStreamFor(chunk, fileNamePart)).thenReturn(chunkOutputStreams.get(chunk));
        }

        var downloadedBytes = downloader.downloadChunks();
        // then
        assertThat(downloadedBytes.getTotalDownloaded()).isEqualTo(expectedContentLength);
        assertThat(downloader.isDone()).isTrue();

        for (var chunk : chunks) {
            try {
                chunkInputStreams.get(chunk).close();
                chunkOutputStreams.get(chunk).close();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

    }

    @Test
    void getDownloadSize() {
        when(downloaderHttpClient.contentLength()).thenReturn(100500L);
        assertThat(downloader.getContentLength()).isEqualTo(100500);
    }

    @Test
    void calculateDownloadRanges() {
        // given
        downloader.setNumberOfThreads(3);
        when(downloaderHttpClient.contentLength()).thenReturn(10L);
        when(downloader.getContentLength()).thenReturn(10L);
        // when
        downloader.calculateChunks();
        var chunks = downloader.getChunks();
        // then
        assertThat(chunks).containsExactly(Chunk.of(0, 3, 0), Chunk.of(4, 7, 1), Chunk.of(8, 9, 2));
    }

}