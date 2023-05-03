package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.service.exception.InvalidUrlException;
import com.blogspot.ostas.downloader.service.exception.MergeChunksException;
import com.blogspot.ostas.downloader.service.exception.NoOutputStreamForChunkException;
import com.blogspot.ostas.downloader.service.exception.SaveFileException;
import com.blogspot.ostas.downloader.service.model.Chunk;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FileService {

  public String filename(String url) {
    try {
      var uri = new URI(url);
      return new File(uri.getPath()).getName();
    } catch (URISyntaxException e) {
      throw new InvalidUrlException(e);
    }
  }

  public void saveToFile(InputStream in, OutputStream out, IntConsumer onBytesReceivedCallback) {
    byte[] buffer = new byte[1024];
    int byteRead;
    try (in; out) {
      do {
        byteRead = in.read(buffer);
        if (byteRead > 0) {
          onBytesReceivedCallback.accept(byteRead);
          out.write(buffer, 0, byteRead);
        }
      } while (byteRead != -1);
    } catch (IOException e) {
      throw new SaveFileException(e);
    }
  }

  public void mergeChunks(Set<Chunk> chunks, String outputFileName) {
    var files = chunks.stream().map(chunk -> outputFileName + "." + chunk.getIndex()).collect(Collectors.toList());
    mergeFiles(files,outputFileName);
  }

  public void mergeFiles(List<String> files, String outputFileName) {
    var filePath = Paths.get(outputFileName);
    try (var targetChannel = FileChannel.open(filePath, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      for (var inputFileName : files) {
        var inputFile = Paths.get(inputFileName);
        try (var inputChannel = FileChannel.open(inputFile, StandardOpenOption.READ)) {
          inputChannel.transferTo(0, inputChannel.size(), targetChannel);
        }
        Files.delete(inputFile);
      }
    } catch (IOException e) {
      throw new MergeChunksException(e);
    }
  }

  public OutputStream outputStreamFor(Chunk chunk, String filename) {
    try {
      return Files.newOutputStream(Paths.get(filename + "." + chunk.getIndex()));
    } catch (IOException exception) {
      throw new NoOutputStreamForChunkException(exception);
    }
  }

  public String chunkFileName(String url, Chunk chunk){
    return "%s.%s".formatted(filename(url),chunk.getIndex());
  }

}
