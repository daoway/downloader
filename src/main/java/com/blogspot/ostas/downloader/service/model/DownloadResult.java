package com.blogspot.ostas.downloader.service.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

@Data
public class DownloadResult {
  private long totalDownloaded;
  private Map<Chunk, Throwable> chunkErrors;

  public DownloadResult() {
    this.chunkErrors = new ConcurrentHashMap<>();
  }

  public boolean hasErrors() {
    return !chunkErrors.entrySet().isEmpty();
  }
}
