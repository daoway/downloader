package com.blogspot.ostas.downloader.persitence.service;

import com.blogspot.ostas.downloader.persitence.model.DownloadTask;
import com.blogspot.ostas.downloader.persitence.repository.DownloadTaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DownloadTaskService {
  private final DownloadTaskRepository downloadTaskRepository;

  @Transactional
  public void addTask(DownloadTask downloadTask) {
    downloadTaskRepository.save(downloadTask);
  }
}
