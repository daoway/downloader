package com.blogspot.ostas.downloader.service;

import static com.blogspot.ostas.downloader.util.Utils.bytesToHumanReadable;

import com.blogspot.ostas.downloader.client.DownloaderHttpClient;
import com.blogspot.ostas.downloader.executor.AppThreadFactory;
import com.blogspot.ostas.downloader.service.model.Chunk;
import com.blogspot.ostas.downloader.service.model.DownloadResult;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
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

  public DownloadResult downloadChunks(
            Set<Chunk> chunks,
            String url,
            ExecutorService executor,
            AtomicLong totalDownloadedBytes,
            int maxThreads
    ) {
        final var semaphore = new Semaphore(maxThreads);
        final var chunkErrors = new ConcurrentHashMap<Chunk, Throwable>();
        final var filename = fileService.filename(url);

        final var futures = new ArrayList<CompletableFuture<Void>>(chunks.size());

        for (Chunk chunk : chunks) {
            var future = CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    fileService.saveToFile(
                            downloaderHttpClient.inputStreamOf(chunk, url),
                            fileService.outputStreamFor(chunk, filename),
                            totalDownloadedBytes::addAndGet
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                }
            }, executor).exceptionally(error -> {
                chunkErrors.put(chunk, error);
                return null;
            });

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        if (!chunkErrors.isEmpty()) {
            int newThreads = chunks.size() - chunkErrors.size();
            log.info("Retry failed chunks with {} threads", newThreads);

            return downloadChunks(
                    chunkErrors.keySet(),
                    url,
                    executor,
                    totalDownloadedBytes,
                    Math.max(1, newThreads)
            );
        }

        var result = new DownloadResult();
        result.setTotalDownloaded(totalDownloadedBytes.get());
        result.setChunkErrors(chunkErrors);
        return result;
    }


    public Set<Chunk> calculateChunks(long contentLength, int numberOfChunks) {
        return rangeService.rangeIntervals(contentLength, numberOfChunks);
  }

    public DownloadResult download(String url) {

        CompletableFuture<Integer> threadsFuture =
                CompletableFuture.supplyAsync(() -> Runtime.getRuntime().availableProcessors());

        CompletableFuture<Long> sizeFuture =
                CompletableFuture.supplyAsync(() -> downloaderHttpClient.contentLength(url));

        CompletableFuture<DownloadResult> resultFuture =
                threadsFuture.thenCombineAsync(sizeFuture, (threads, size) -> {

                    log.info("Downloading total bytes {} (~{})",
                            size, bytesToHumanReadable(size));

                    var executor = Executors.newFixedThreadPool(threads, new AppThreadFactory());
                    var totalDownloaded = new AtomicLong(0);
                    var chunks = calculateChunks(size, threads);

                    try {
                        var result = downloadChunks(
                                chunks, url, executor, totalDownloaded, threads
                        );

                        if (!result.hasErrors()) {
                            var filename = fileService.filename(url);
                            fileService.mergeChunks(chunks, filename);
                        }

                        return result;
                    } finally {
                        executor.shutdown();
                    }
                });

        try {
            return resultFuture.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;

        } catch (ExecutionException e) {
            log.error("Execution error", e);
            return null;
        }
    }
}
