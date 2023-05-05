package com.blogspot.ostas.downloader.persitence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.blogspot.ostas.downloader.persitence.model.UrlItem;
import com.blogspot.ostas.downloader.persitence.model.DownloadTask;
import com.blogspot.ostas.downloader.persitence.model.enums.TaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"command.line.runner.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:file:./data/download_test_db"})
class DownloadTaskRepositoryTest {
  @Autowired
  private DownloadTaskRepository downloadTaskRepository;

  @Test
  void testPersist() {
    var task = new DownloadTask();
    task.setStatus(TaskStatus.IN_PROGRESS);
    var url0 = "http://localhost/bigfile_0";
    var priority0 = 0;
    var item0 = UrlItem.of(url0, priority0);

    task.setUrls(List.of(item0));
    downloadTaskRepository.save(task);
    assertThat(task.getId()).isNotNull();
  }
}