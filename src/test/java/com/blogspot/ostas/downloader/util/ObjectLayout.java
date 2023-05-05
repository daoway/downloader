package com.blogspot.ostas.downloader.util;

import com.blogspot.ostas.downloader.persitence.model.UrlItem;
import com.blogspot.ostas.downloader.persitence.model.DownloadTask;
import com.blogspot.ostas.downloader.persitence.model.enums.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

@Slf4j
public class ObjectLayout {
  public static void main(String[] args) {
    DownloadTask downloadTask = new DownloadTask();
    downloadTask.setId(1L);
    downloadTask.getUrls().add(UrlItem.of("http://localhoost:8080/1",1));
    downloadTask.getUrls().add(UrlItem.of("http://localhoost:8080/2",2));
    downloadTask.getUrls().add(UrlItem.of("http://localhoost:8080/3",3));
    downloadTask.setStatus(TaskStatus.IN_PROGRESS);

    log.info(ClassLayout.parseInstance(downloadTask).toPrintable());
    log.info(GraphLayout.parseInstance(downloadTask).toFootprint());
  }
}
