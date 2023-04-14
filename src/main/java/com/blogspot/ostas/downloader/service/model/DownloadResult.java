package com.blogspot.ostas.downloader.service.model;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class DownloadResult {
    private long totalDownloaded;
    private Map<Chunk, Throwable> chunksErrors;

    public DownloadResult() {
        this.chunksErrors = new ConcurrentHashMap<>();
    }

    public boolean hasErrors() {
        return !chunksErrors.entrySet().isEmpty();
    }
}
