package com.blogspot.ostas.downloader.client.exception;

public class NoContentLengthException extends RuntimeException {
  public NoContentLengthException(String message) {
    super(message);
  }

  public NoContentLengthException(Throwable t) {
    super(t);
  }

}
