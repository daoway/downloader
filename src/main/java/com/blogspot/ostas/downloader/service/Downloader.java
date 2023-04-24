package com.blogspot.ostas.downloader.service;

import static com.blogspot.ostas.downloader.util.Utils.bytesToHumanReadable;

import com.blogspot.ostas.downloader.client.DownloaderHttpClient;
import com.blogspot.ostas.downloader.executor.AppThreadFactory;
import com.blogspot.ostas.downloader.service.model.Chunk;
import com.blogspot.ostas.downloader.service.model.DownloadResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Downloader {

  private final DownloaderHttpClient downloaderHttpClient;

  private final RangeService rangeService;

  private final FileService fileService;

  private final AtomicLong totalDownloadedBytes = new AtomicLong(0);
  private ExecutorService executor;
  private volatile boolean done;
  private long contentLength = -1;
  private List<Chunk> chunks;

  public int setNumberOfThreads(int numberOfThreads) {
    this.executor = Executors.newFixedThreadPool(numberOfThreads, new AppThreadFactory());
    return numberOfThreads;
  }

  public void handleChunkDownloadedBytesCount(int byteRead) {
    long current;
    long next;
    do {
      current = totalDownloadedBytes.get();
      next = current + byteRead;
    } while (!totalDownloadedBytes.compareAndSet(current, next));
  }

  public DownloadResult downloadChunks(List<Chunk> chunks) {
    final var chunkErrors = new ConcurrentHashMap<Chunk, Throwable>();
    final var downloadResult = new DownloadResult();
    final var filename = fileService.filename(downloaderHttpClient.getUrl());
    final var futureChunks = new ArrayList<CompletableFuture<Void>>(chunks.size());
    chunks.forEach(chunk -> {
      var future = CompletableFuture.runAsync(
          () -> fileService.saveToFile(downloaderHttpClient.inputStreamOf(chunk),
              fileService.outputStreamFor(chunk, filename), this::handleChunkDownloadedBytesCount),
          executor).exceptionally(error -> {
        chunkErrors.put(chunk, error);
        return null;
      });
      futureChunks.add(future);
    });
    CompletableFuture.allOf(futureChunks.toArray(CompletableFuture[]::new)).join();
    executor.shutdown();
    done = true;
    downloadResult.setTotalDownloaded(totalDownloadedBytes.get());
    downloadResult.setChunkErrors(chunkErrors);
    return downloadResult;
  }

  public boolean isDone() {
    return done;
  }

  public long getContentLength() {
    if (this.contentLength == -1) {
      contentLength = downloaderHttpClient.contentLength();
    }
    return this.contentLength;
  }

  public List<Chunk> calculateChunks(long contentLength, int numberOfChunks) {
    this.chunks = rangeService.rangeIntervals(contentLength, numberOfChunks);
    return this.chunks;
  }

  public DownloadResult download(String url) {
    downloaderHttpClient.setUrl(url);
    CompletableFuture<Integer> threadsNumberFuture =
        CompletableFuture.supplyAsync(
            () -> setNumberOfThreads(Runtime.getRuntime().availableProcessors()));
    CompletableFuture<Long> contentLengthFuture =
        CompletableFuture.supplyAsync(this::getContentLength);
    CompletableFuture<DownloadResult> downloadSteps =
        threadsNumberFuture.thenCombineAsync(contentLengthFuture,
            (threadsNumber, downloadSize) -> {
              log.info("Downloading total bytes {} (~{})", downloadSize,
                  bytesToHumanReadable(downloadSize));
              return calculateChunks(downloadSize, threadsNumber);
            }).thenApplyAsync(this::downloadChunks);
    try {
      var downloadResult = downloadSteps.get();
      if (downloadResult.hasErrors()) {
        return downloadResult;
      } else {
        var filename = fileService.filename(downloaderHttpClient.getUrl());
        fileService.mergeChunks(chunks, filename);
      }
      return downloadResult;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException exception) {
      log.error("Execution error", exception);
    }
    return null;
  }

  public List<Chunk> getChunks() {
    return Collections.unmodifiableList(chunks);
  }

}
