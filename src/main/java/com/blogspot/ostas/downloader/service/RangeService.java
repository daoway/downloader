package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.service.model.Chunk;

import java.util.Set;

public interface RangeService {
    Set<Chunk> rangeIntervals(long value, int numberOfChunks);
}
