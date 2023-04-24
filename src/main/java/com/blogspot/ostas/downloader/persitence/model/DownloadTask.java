package com.blogspot.ostas.downloader.persitence.model;

import com.blogspot.ostas.downloader.persitence.model.enums.DownloadTaskStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;
import lombok.Data;

@Data
@Entity
public class DownloadTask {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  @Enumerated
  private DownloadTaskStatus status;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DownloadItem> downloadUrls;
}
