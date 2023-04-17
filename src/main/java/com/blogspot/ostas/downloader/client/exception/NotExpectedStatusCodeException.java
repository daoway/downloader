package com.blogspot.ostas.downloader.client.exception;

import lombok.Getter;

@Getter
public class NotExpectedStatusCodeException extends RuntimeException {
  private final int statusCode;

  public NotExpectedStatusCodeException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

}