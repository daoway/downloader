package com.blogspot.ostas.downloader.service.impl;

import com.blogspot.ostas.downloader.client.DownloaderHttpClient;
import com.blogspot.ostas.downloader.service.Downloader;
import com.blogspot.ostas.downloader.service.FileService;
import com.blogspot.ostas.downloader.service.RangeService;
import com.blogspot.ostas.downloader.service.model.Chunk;
import com.blogspot.ostas.downloader.service.model.DownloadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.blogspot.ostas.downloader.util.Utils.bytesToHumanReadable;

@Component
@RequiredArgsConstructor
@Slf4j
public class DownloaderImpl implements Downloader {

    private final DownloaderHttpClient downloaderHttpClient;
    private final RangeService rangeService;
    private final FileService fileService;
    private final ExecutorService downloaderExecutor;

    @Override
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
            int newThreads = Math.max(1, chunks.size() - chunkErrors.size());
            log.info("Retry failed chunks with {} threads", newThreads);

            return downloadChunks(
                    chunkErrors.keySet(),
                    url,
                    executor,
                    totalDownloadedBytes,
                    newThreads
            );
        }

        var result = new DownloadResult();
        result.setTotalDownloaded(totalDownloadedBytes.get());
        result.setChunkErrors(chunkErrors);
        return result;
    }

    @Override
    public Set<Chunk> calculateChunks(long contentLength, int numberOfChunks) {
        return rangeService.rangeIntervals(contentLength, numberOfChunks);
    }

    @Override
    public DownloadResult download(String url) {

        CompletableFuture<Integer> threadsFuture =
                CompletableFuture.supplyAsync(() -> Runtime.getRuntime().availableProcessors());

        CompletableFuture<Long> sizeFuture =
                CompletableFuture.supplyAsync(() -> downloaderHttpClient.contentLength(url));

        CompletableFuture<DownloadResult> resultFuture =
                threadsFuture.thenCombineAsync(sizeFuture, (threads, size) -> {

                    log.info("Downloading total bytes {} (~{})",
                            size, bytesToHumanReadable(size));

                    var totalDownloaded = new AtomicLong(0);
                    var chunks = calculateChunks(size, threads);
                    var result = downloadChunks(
                            chunks, url, downloaderExecutor, totalDownloaded, threads
                    );

                    if (!result.hasErrors()) {
                        var filename = fileService.filename(url);
                        fileService.mergeChunks(chunks, filename);
                    }

                    return result;
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
