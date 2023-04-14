package com.blogspot.ostas.downloader.service.exception;

public class NoOutputStreamForChunkException extends RuntimeException {

    public NoOutputStreamForChunkException(Throwable t) {
        super(t);
    }

}
