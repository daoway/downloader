package com.blogspot.ostas.downloader.persitence.repository;

import com.blogspot.ostas.downloader.persitence.model.DownloadTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DownloadTaskRepository extends JpaRepository<DownloadTask, Long> {
}
