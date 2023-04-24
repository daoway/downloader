package com.blogspot.ostas.downloader.persitence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class DownloadItem {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  private String url;
  private Long priority;

  private DownloadItem(String url, Long priority) {
    this.url = url;
    this.priority = priority;
  }

  public static DownloadItem of(String url, Long priority) {
    return new DownloadItem(url, priority);
  }
}
