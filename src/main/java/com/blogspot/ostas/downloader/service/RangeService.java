package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.service.model.Chunk;
import com.google.common.math.LongMath;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RangeService {

  public List<Chunk> rangeIntervals(long value, int numberOfChunks) {
    var chunkSize = (int) Math.ceil((double) value / numberOfChunks);
    var chunks = new ArrayList<Chunk>(numberOfChunks);
    for (int i = 0; i < numberOfChunks; i++) {
      long start = LongMath.checkedMultiply(i, chunkSize);
      long end = Math.min(start + chunkSize - 1, value - 1);
      chunks.add(new Chunk(start, end, i));
    }
    return chunks;
  }

}
