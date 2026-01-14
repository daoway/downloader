package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.service.model.Chunk;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

public interface FileService {
    String filename(String url);

    void saveToFile(InputStream in, OutputStream out, IntConsumer onBytesReceivedCallback);

    void mergeChunks(Set<Chunk> chunks, String outputFileName);

    void mergeFiles(List<String> files, String outputFileName);

    OutputStream outputStreamFor(Chunk chunk, String filename);
}
