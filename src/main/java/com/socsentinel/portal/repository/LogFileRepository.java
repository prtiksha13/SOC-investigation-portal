package com.socsentinel.portal.repository;

import com.socsentinel.portal.model.LogFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogFileRepository extends JpaRepository<LogFile, Long> {
    List<LogFile> findAllByOrderByUploadTimeDesc();
}
