package com.blogspot.ostas.downloader.client;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

import com.blogspot.ostas.downloader.client.exception.NoContentLengthException;
import com.blogspot.ostas.downloader.client.exception.NoRangeStreamException;
import com.blogspot.ostas.downloader.client.exception.FileNotFoundException;
import com.blogspot.ostas.downloader.client.exception.NotExpectedStatusCodeException;
import com.blogspot.ostas.downloader.service.FileService;
import com.blogspot.ostas.downloader.service.model.Chunk;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Data
@Component
@RequiredArgsConstructor
@Slf4j
public class DownloaderHttpClient {

  private final FileService fileService;

  private final HttpClient httpClient;

  private String url;

  public long contentLength() {
    HttpRequest httpRequest = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .method("HEAD", HttpRequest.BodyPublishers.noBody())
        .build();
    try {
      HttpResponse<Void> response =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
      log.info("Status code : {}",response.statusCode());
      if(response.statusCode() == HTTP_NOT_FOUND) throw new FileNotFoundException("Wrong url or file not found. Got 404 http status code.");
      if(response.statusCode() == HTTP_UNAVAILABLE) throw new NoContentLengthException("503 http status code obtaining content length");
      if(response.statusCode() == HTTP_FORBIDDEN) throw new NoContentLengthException("403 http status code");
      return response.headers()
          .firstValueAsLong("Content-Length")
          .orElseThrow(() -> new RuntimeException("Content length header not set"));
    } catch (IOException e) {
      throw new NoContentLengthException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return -1;
  }

  public InputStream inputStreamOf(Chunk chunk) {
    HttpRequest httpRequest = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Range", "bytes=%s-%s".formatted(chunk.start(), chunk.end()))
        .GET()
        .build();
    try {
      HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest,
          HttpResponse.BodyHandlers.ofInputStream());
      int statusCode = httpResponse.statusCode();
      if (!(statusCode == 206 || statusCode == 200)) {
        throw new NotExpectedStatusCodeException(
            "Not expected status code %s".formatted(statusCode), statusCode);
      }
      return httpResponse.body();
    } catch (IOException e) {
      throw new NoRangeStreamException(e);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }
    return null;
  }

}
