package com.socsentinel.portal.repository;
import java.util.List;

 import org.springframework.data.jpa.repository.JpaRepository;

 import com.socsentinel.portal.model.Alert;
import com.socsentinel.portal.model.AlertStatus;
import com.socsentinel.portal.model.RiskLevel;
import com.socsentinel.portal.model.Severity;
public interface AlertRepository extends JpaRepository<Alert, Long> {
  List<Alert> findAllByOrderByCreatedAtDesc();
  long countBySeverity(Severity s);
  long countBySeverityAndStatusNot(Severity s, AlertStatus status);
  long countByRiskLevelAndStatusNot(RiskLevel r, AlertStatus status);
  long countByStatus(AlertStatus status);
  long countByStatusNot(AlertStatus status);
}
