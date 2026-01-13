package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.service.model.Chunk;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class RangeService {

    public Set<Chunk> rangeIntervals(long value, int numberOfChunks) {
        if (value <= 0 || numberOfChunks <= 0) {
            throw new IllegalArgumentException("Invalid range");
        }
        var chunkSize = (long) Math.ceil((double) value / numberOfChunks);
        Set<Chunk> chunks = LinkedHashSet.newLinkedHashSet(numberOfChunks);
        long start;
        long end;
        for (int i = 0; i < numberOfChunks; i++) {
            start = i * chunkSize;
            if (start >= value) break;
            end = Math.min(start + chunkSize - 1, value - 1);
            chunks.add(Chunk.of(start, end, i));
        }
        return chunks;
    }

}
