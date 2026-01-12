package com.blogspot.ostas.downloader.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.blogspot.ostas.downloader.client.DownloaderHttpClient;
import com.blogspot.ostas.downloader.service.model.Chunk;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
  void download() {
    // given
    var url = "http://dummy-url-used-for-testing.com/test.out";
    long contentLength = 224;
    var threadsCount = downloader.setNumberOfThreads(Runtime.getRuntime().availableProcessors());

    // when
    when(downloaderHttpClient.getUrl()).thenReturn(url);
    when(downloaderHttpClient.contentLength()).thenReturn(contentLength);
    var chunks = downloader.calculateChunks(contentLength, threadsCount);
    Map<Chunk, InputStream> chunkInputStreams = new ConcurrentHashMap<>();
    Map<Chunk, OutputStream> chunkOutputStreams = new ConcurrentHashMap<>();
    int expectedContentLength = 0;
    var fileName = fileService.filename(downloaderHttpClient.getUrl());
    for (var chunk : chunks) {
      byte[] chunkBytes = "contents of chunk number %s%n".formatted(chunk.index())
          .getBytes(StandardCharsets.UTF_8);
      expectedContentLength += chunkBytes.length;
      chunkInputStreams.put(chunk, new ByteArrayInputStream(chunkBytes));
      chunkOutputStreams.put(chunk, new ByteArrayOutputStream());
      when(downloaderHttpClient.inputStreamOf(chunk)).thenReturn(chunkInputStreams.get(chunk));
      when(fileService.outputStreamFor(chunk, fileName)).thenReturn(
          chunkOutputStreams.get(chunk));
    }

    var downloadResult = downloader.downloadChunks(chunks);
    // then
    assertThat(downloadResult.getTotalDownloaded()).isEqualTo(expectedContentLength);

    for (var chunk : chunks) {
      try {
        chunkInputStreams.get(chunk).close();
        chunkOutputStreams.get(chunk).close();
        Files.delete(Path.of("%s.%d".formatted(fileName, chunk.index())));
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
    var threadsCount = downloader.setNumberOfThreads(3);
    when(downloaderHttpClient.contentLength()).thenReturn(10L);
    when(downloader.getContentLength()).thenReturn(10L);
    // when
    downloader.calculateChunks(downloaderHttpClient.contentLength(), threadsCount);
    var chunks = downloader.getChunks();
    // then
    assertThat(chunks).containsExactly(Chunk.of(0, 3, 0), Chunk.of(4, 7, 1), Chunk.of(8, 9, 2));
  }

  @BeforeEach
  public void resetMocks() {
    Mockito.reset(downloader); // reset the spy object
    downloader = Mockito.spy(downloader); // re-spy the bean
  }

}
