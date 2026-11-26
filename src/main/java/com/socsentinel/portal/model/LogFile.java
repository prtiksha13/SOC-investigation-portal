package com.socsentinel.portal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_files")
public class LogFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private Integer totalLines;

    @Column(nullable = false)
    private Integer alertCount;

    @Column(nullable = false)
    private LocalDateTime uploadTime = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getTotalLines() { return totalLines; }
    public void setTotalLines(Integer totalLines) { this.totalLines = totalLines; }

    public Integer getAlertCount() { return alertCount; }
    public void setAlertCount(Integer alertCount) { this.alertCount = alertCount; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
}
