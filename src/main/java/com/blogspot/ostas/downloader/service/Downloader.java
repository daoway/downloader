package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.service.model.Chunk;
import com.blogspot.ostas.downloader.service.model.DownloadResult;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public interface Downloader {
    DownloadResult downloadChunks(
            Set<Chunk> chunks,
            String url,
            ExecutorService executor,
            AtomicLong totalDownloadedBytes,
            int maxThreads
    );

    Set<Chunk> calculateChunks(long contentLength, int numberOfChunks);

    DownloadResult download(String url);
}
