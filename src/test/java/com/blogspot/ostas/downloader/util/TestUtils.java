package com.blogspot.ostas.downloader.util;

import static com.blogspot.ostas.downloader.util.Utils.bytesToHumanReadable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@SuppressWarnings("PMD")
public class TestUtils {

  public static void main(String[] args) {
    int linesCount = 100_000;
    //generateBigFile(linesCount, "src/test/resources/public/downloads/file.out");
    createFileOfSize("test.raw",10737418240L); //10 Gb
  }

  private static void generateBigFile(int linesCount, String fileName) {
    try (var bigFile = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
      var line = "1".repeat(100);
      for (int i = 0; i < linesCount; i++) {
        bigFile.write(line + "\n");
      }
    } catch (IOException e) {
      log.error("Unable to generate big file : ", e);
      return;
    }
    long size = new File(fileName).length();
    log.info("size : " + bytesToHumanReadable(size));
  }

  public static void downloadFile(String url, String filename, HttpClient httpClient) {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    HttpResponse<InputStream> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    if (response.statusCode() != HttpStatus.OK.value()) {
      throw new DownloadException("Non OK code : " + response.statusCode());
    }
    try (InputStream inputStream = response.body();
         var outputStream = Files.newOutputStream(Paths.get(filename))) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      do {
        bytesRead = inputStream.read(buffer);
        if (bytesRead > 0) {
          outputStream.write(buffer, 0, bytesRead);
        }
      } while (bytesRead != -1);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @SuppressWarnings("PMD.AvoidCatchingThrowable")
  public static List<Throwable> concurrentDownload(String url, int numberOfThreads,
                                                   BiConsumer<String, Integer> action) {
    final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
    final CountDownLatch fire = new CountDownLatch(1);
    final Thread[] threads = new Thread[numberOfThreads];
    IntStream.rangeClosed(0, numberOfThreads - 1).forEach(index -> {
      threads[index] = new Thread(() -> {
        try {
          fire.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        try {
          action.accept(url, index);
        } catch (Throwable throwable) {
          synchronized (errors) {
            errors.add(throwable);
          }
        }
      });
    });
    IntStream.rangeClosed(0, numberOfThreads - 1).forEach(index -> {
      threads[index].start();
    });
    fire.countDown();
    IntStream.rangeClosed(0, numberOfThreads - 1).forEach(index -> {
      try {
        threads[index].join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    return errors;
  }

  public static void createFileOfSize(String fileName, long fileSize) {
    File file = new File(fileName);
    try (var raf = new RandomAccessFile(file, "rw")) {
      raf.setLength(fileSize);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }
}
