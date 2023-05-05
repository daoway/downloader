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
public class UrlItem {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  private String url;
  private Integer priority;

  private UrlItem(String url, Integer priority) {
    this.url = url;
    this.priority = priority;
  }

  public static UrlItem of(String url, Integer priority) {
    return new UrlItem(url, priority);
  }
}
