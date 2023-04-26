package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.service.model.Chunk;
import com.google.common.math.LongMath;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RangeService {

  public Set<Chunk> rangeIntervals(long value, int numberOfChunks) {
    var chunkSize = (int) Math.ceil((double) value / numberOfChunks);
    var chunks = new LinkedHashSet<Chunk>(numberOfChunks);
    for (int i = 0; i < numberOfChunks; i++) {
      long start = LongMath.checkedMultiply(i, chunkSize);
      long end = Math.min(start + chunkSize - 1, value - 1);
      chunks.add(new Chunk(start, end, i));
    }
    return chunks;
  }

}
