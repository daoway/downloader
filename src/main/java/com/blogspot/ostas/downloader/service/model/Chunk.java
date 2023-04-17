package com.blogspot.ostas.downloader.service.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@EqualsAndHashCode
@RequiredArgsConstructor
public class Chunk {

  private final long start;

  private final long end;

  private final long index;

  public static Chunk of(long start, long end, long index) {
    return new Chunk(start, end, index);
  }

  @Override
  public String toString() {
    return "[%d..%d]".formatted(start, end);
  }

}
