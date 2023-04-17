package com.blogspot.ostas.downloader.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"command.line.runner.enabled=false"})
class FileServiceTest {

  @Autowired
  private FileService fileService;

  @Test
  void filename() {
    var url = "https://dlcdn.apache.org/spark/spark-3.3.2/spark-3.3.2-bin-hadoop3.tgz";
    var fileName = fileService.filename(url);
    assertThat(fileName).isEqualTo("spark-3.3.2-bin-hadoop3.tgz");
  }
}