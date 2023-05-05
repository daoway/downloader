package com.blogspot.ostas.downloader.service.model;

public record Chunk(long start, long end, int index) {

  public static Chunk of(long start, long end, int index) {
    return new Chunk(start, end, index);
  }

  @Override
  public String toString() {
    return "[%d..%d]".formatted(start, end);
  }

}
