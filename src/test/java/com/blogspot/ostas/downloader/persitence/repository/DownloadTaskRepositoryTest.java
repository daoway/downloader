package com.blogspot.ostas.downloader.persitence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.blogspot.ostas.downloader.persitence.model.DownloadItem;
import com.blogspot.ostas.downloader.persitence.model.DownloadTask;
import com.blogspot.ostas.downloader.persitence.model.enums.DownloadTaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"command.line.runner.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"})
class DownloadTaskRepositoryTest {
  @Autowired
  private DownloadTaskRepository downloadTaskRepository;

  @Test
  void testPersist() {
    var task = new DownloadTask();
    task.setStatus(DownloadTaskStatus.IN_PROGRESS);
    var url0 = "http://localhost/bigfile_0";
    var priority0 = 0L;
    var item0 = DownloadItem.of(url0, priority0);

    task.setDownloadUrls(List.of(item0));
    downloadTaskRepository.save(task);
    assertThat(task.getId()).isNotNull();
  }
}