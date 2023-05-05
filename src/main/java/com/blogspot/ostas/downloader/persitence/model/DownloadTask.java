package com.blogspot.ostas.downloader.persitence.model;

import com.blogspot.ostas.downloader.persitence.model.enums.TaskStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Entity
public class DownloadTask {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  @Enumerated
  private TaskStatus status;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UrlItem> urls = new ArrayList<>();
}
