package com.socsentinel.portal.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.socsentinel.portal.model.Alert;
import com.socsentinel.portal.model.AlertStatus;
import com.socsentinel.portal.model.AlertType;
import com.socsentinel.portal.model.AttackType;
import com.socsentinel.portal.model.InvestigationEvent;
import com.socsentinel.portal.model.InvestigationNote;
import com.socsentinel.portal.model.LogFile;
import com.socsentinel.portal.model.RiskLevel;
import com.socsentinel.portal.model.Severity;
import com.socsentinel.portal.repository.AlertRepository;
import com.socsentinel.portal.repository.InvestigationNoteRepository;
import com.socsentinel.portal.repository.LogFileRepository;
import com.socsentinel.portal.service.InvestigationHistoryService;
import com.socsentinel.portal.service.LogAnalysisService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api")
public class SocController {
  private final AlertRepository alerts;
  private final InvestigationNoteRepository notes;
  private final LogFileRepository logFiles;
  private final LogAnalysisService logAnalysisService;
  private final InvestigationHistoryService investigationHistory;

  public SocController(AlertRepository alerts, InvestigationNoteRepository notes, LogFileRepository logFiles,
                       LogAnalysisService logAnalysisService, InvestigationHistoryService investigationHistory){
    this.alerts=alerts;
    this.notes=notes;
    this.logFiles=logFiles;
    this.logAnalysisService=logAnalysisService;
    this.investigationHistory=investigationHistory;
  }

  @GetMapping("/alerts")
  public List<AlertResponse> allAlerts(){
    return alerts.findAllByOrderByCreatedAtDesc().stream().map(AlertResponse::from).toList();
  }

  @DeleteMapping("/alerts")
  public Map<String, Object> clearAlertsForNewUploadCycle() {
    notes.deleteAll();
    alerts.deleteAll();
    return Map.of("status", "ok", "message", "Alert data cleared. Upload a log file to generate fresh alerts.");
  }

  @PostMapping("/alerts/cleanup-demo-data")
  public Map<String, Object> cleanupDemoData() {
    return clearAlertsForNewUploadCycle();
  }

  @PostMapping("/alerts")
  @ResponseStatus(HttpStatus.CREATED)
  public AlertResponse createAlert(@Valid @RequestBody AlertRequest request){
    Alert a=new Alert();
    a.setReferenceCode("ALT-"+(2043+alerts.count()+1));
    apply(a,request);
    Alert saved = alerts.save(a);
    investigationHistory.record(saved, com.socsentinel.portal.model.InvestigationEventType.ALERT_CREATED, "system", "SYSTEM");
    return AlertResponse.from(saved);
  }

  @PatchMapping("/alerts/{id}/status")
  public AlertResponse updateStatus(Authentication auth, @PathVariable Long id,@RequestBody StatusRequest request){
    if (hasRole(auth, "ROLE_ANALYST_L1") && !isL1Status(request.status())) {
      throw new AccessDeniedException("Access Denied");
    }
    Alert a=findAlert(id);
    AlertStatus oldStatus = a.getStatus();
    a.setStatus(request.status());
    Alert saved = alerts.save(a);
    investigationHistory.statusChanged(saved, auth.getName(), roleName(auth), oldStatus, request.status());
    return AlertResponse.from(saved);
  }

  private boolean hasRole(Authentication auth, String role) {
    return auth != null && auth.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()));
  }

  private String roleName(Authentication auth) {
    return auth == null ? "UNKNOWN" : auth.getAuthorities().stream()
        .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
        .findFirst().orElse("UNKNOWN");
  }

  private boolean isL1Status(AlertStatus status) {
    return status == AlertStatus.OPEN || status == AlertStatus.INVESTIGATING || status == AlertStatus.UNDER_INVESTIGATION;
  }

  @PostMapping("/alerts/{id}/escalate")
  public AlertResponse escalateAlert(Authentication auth, @PathVariable Long id, @RequestBody(required = false) EscalationRequest request) {
    if (hasRole(auth, "ROLE_ANALYST_L1")) {
      Alert alert = findAlert(id);
      alert.setStatus(AlertStatus.UNDER_INVESTIGATION);
      Alert saved = alerts.save(alert);
      String reason = request == null ? "L1 escalation requested" : request.reason();
      investigationHistory.escalated(saved, auth.getName(), roleName(auth), reason);
      investigationHistory.assigned(saved, auth.getName(), roleName(auth), "analyst2", "ANALYST_L2");
      return AlertResponse.from(saved);
    }
    throw new AccessDeniedException("Access Denied");
  }

  @GetMapping("/notes")
  public List<NoteResponse> allNotes(){
    return notes.findAllByOrderByCreatedAtDesc().stream().map(NoteResponse::from).toList();
  }

  @PostMapping("/notes")
  @ResponseStatus(HttpStatus.CREATED)
  public NoteResponse addNote(Authentication auth,@Valid @RequestBody NoteRequest request){
    InvestigationNote n=new InvestigationNote();
    n.setAlert(findAlert(request.alertId()));
    n.setText(request.text());
    String analyst = auth != null ? auth.getName() : "SOC Analyst";
    n.setAnalystName(analyst);
    InvestigationNote saved = notes.save(n);
    investigationHistory.noteAdded(saved.getAlert(), analyst, roleName(auth), saved.getText());
    return NoteResponse.from(saved);
  }

  @PostMapping("/alerts/{id}/investigations")
  public InvestigationEventResponse saveInvestigation(Authentication auth, @PathVariable Long id,
                                                       @Valid @RequestBody InvestigationRequest request) {
    if (auth == null || (!hasRole(auth, "ROLE_ANALYST_L1") && !hasRole(auth, "ROLE_ANALYST_L2") && !hasRole(auth, "ROLE_ADMIN"))) {
      throw new AccessDeniedException("Access Denied");
    }
    Alert alert = findAlert(id);
    if (hasRole(auth, "ROLE_ANALYST_L1") && alert.getStatus() == AlertStatus.UNDER_INVESTIGATION) {
      throw new AccessDeniedException("Access Denied");
    }
    InvestigationEvent event = investigationHistory.investigationSaved(alert, auth.getName(), roleName(auth),
        request.notes(), request.actionsPerformed(), request.findings(), request.conclusion(), request.finalDecision());
    return InvestigationEventResponse.from(event);
  }

  @GetMapping("/alerts/{id}/timeline")
  public List<InvestigationEventResponse> timeline(Authentication auth, @PathVariable Long id) {
    if (auth == null || (!hasRole(auth, "ROLE_ANALYST_L1") && !hasRole(auth, "ROLE_ANALYST_L2") && !hasRole(auth, "ROLE_ADMIN"))) {
      throw new AccessDeniedException("Access Denied");
    }
    findAlert(id);
    return investigationHistory.timeline(id).stream().map(InvestigationEventResponse::from).toList();
  }

  @PatchMapping("/alerts/{id}/severity")
  public AlertResponse updateSeverity(Authentication auth, @PathVariable Long id, @Valid @RequestBody SeverityRequest request) {
    if (!hasRole(auth, "ROLE_ANALYST_L2") && !hasRole(auth, "ROLE_ADMIN")) {
      throw new AccessDeniedException("Access Denied");
    }
    Alert alert = findAlert(id);
    Severity oldSeverity = alert.getSeverity();
    alert.setSeverity(request.severity());
    alert.setRiskLevel(RiskLevel.from(alert.getPriorityScore(), request.severity()));
    Alert saved = alerts.save(alert);
    investigationHistory.severityChanged(saved, auth.getName(), roleName(auth), oldSeverity, request.severity());
    return AlertResponse.from(saved);
  }

  @GetMapping("/metrics")
  public Map<String,Object> metrics(){
    long totalLogs = logFiles.findAll().stream().mapToLong(log -> log.getTotalLines() == null ? 0 : log.getTotalLines()).sum();
    long totalAlerts = alerts.count();
    Map<String, Map<String, Long>> statusCategories = statusCategoryCounts();
    Map<String,Object> metrics = new LinkedHashMap<>();
    metrics.put("totalLogs", totalLogs);
    metrics.put("threatsDetected", totalAlerts);
    metrics.put("totalAlerts", totalAlerts);
    metrics.put("critical", alerts.countBySeverity(Severity.CRITICAL));
    metrics.put("high", alerts.countBySeverity(Severity.HIGH));
    metrics.put("medium", alerts.countBySeverity(Severity.MEDIUM));
    metrics.put("low", alerts.countBySeverity(Severity.LOW));
    metrics.put("activeAlerts", alerts.findAll().stream().filter(a -> a.getStatus() != AlertStatus.RESOLVED && a.getStatus() != AlertStatus.CLOSED).count());
    metrics.put("open", alerts.countByStatus(AlertStatus.OPEN));
    metrics.put("investigating", alerts.findAll().stream().filter(a -> a.getStatus() == AlertStatus.INVESTIGATING || a.getStatus() == AlertStatus.UNDER_INVESTIGATION).count());
    metrics.put("resolved", alerts.countByStatus(AlertStatus.RESOLVED));
    metrics.put("closed", alerts.countByStatus(AlertStatus.CLOSED));
    metrics.put("statusCategories", statusCategories);
    return metrics;
  }

  private Map<String, Map<String, Long>> statusCategoryCounts() {
    List<String> statuses = List.of("OPEN", "INVESTIGATING", "RESOLVED", "CLOSED");
    List<AlertType> categories = List.of(AlertType.AUTHENTICATION, AlertType.AUTHORIZATION, AlertType.APPLICATION,
        AlertType.NETWORK, AlertType.SYSTEM, AlertType.SECURITY);
    Map<String, Map<String, Long>> counts = new LinkedHashMap<>();
    for (String status : statuses) {
      Map<String, Long> categoryCounts = new LinkedHashMap<>();
      for (AlertType category : categories) categoryCounts.put(category.name(), 0L);
      counts.put(status, categoryCounts);
    }
    alerts.findAll().forEach(alert -> {
      String status = displayStatus(alert.getStatus());
      AlertType category = categoryFor(alert);
      if (counts.containsKey(status)) counts.get(status).compute(category.name(), (key, value) -> value + 1);
    });
    return counts;
  }

  private static AlertType categoryFor(AlertType type) {
    return switch (type) {
      case ENDPOINT -> AlertType.SYSTEM;
      case POLICY_VIOLATION -> AlertType.AUTHORIZATION;
      case DATA_EXFILTRATION, OTHER -> AlertType.SECURITY;
      default -> type;
    };
  }

  private static AlertType categoryFor(Alert alert) {
    String event = (alert.getTitle() + " " + (alert.getMatchedLogEntry() == null ? "" : alert.getMatchedLogEntry())).toLowerCase();
    return switch (alert.getAttackType()) {
      case FAILED_LOGIN -> AlertType.AUTHENTICATION;
      case BRUTE_FORCE -> event.contains("failed login") || event.contains("authentication failed")
          ? AlertType.AUTHENTICATION : AlertType.SECURITY;
      case PRIVILEGE_ESCALATION, UNAUTHORIZED_ACCESS -> AlertType.AUTHORIZATION;
      case SQL_INJECTION, XSS, DIRECTORY_TRAVERSAL -> AlertType.APPLICATION;
      case PORT_SCAN, SUSPICIOUS_IP, SSH_ATTACK -> AlertType.NETWORK;
      case MALWARE, RANSOMWARE -> AlertType.SECURITY;
      case COMMAND_INJECTION -> event.contains("powershell") || event.contains("encodedcommand")
          ? AlertType.SYSTEM : AlertType.APPLICATION;
      default -> categoryFor(alert.getAlertType());
    };
  }

  private static String displayStatus(AlertStatus status) {
    return status == AlertStatus.UNDER_INVESTIGATION ? "INVESTIGATING" : status.name();
  }

  @GetMapping("/user")
  public UserResponse currentUser(Authentication auth){
    if (auth == null) throw new IllegalStateException("Unauthenticated");
    List<String> roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
    return new UserResponse(auth.getName(), roles);
  }

  @PostMapping("/alerts/{id}/ai-analysis")
  public AiResponse aiAnalysis(@PathVariable Long id){
    Alert a=findAlert(id);
    String concern=a.getSeverity()==Severity.CRITICAL?"Potential active compromise requiring immediate containment.":"Suspicious activity that needs validation against normal operations.";
    String confidence=a.getPriorityScore()>=90?"High":a.getPriorityScore()>=70?"Medium-high":"Moderate";
    return new AiResponse(confidence,concern,"Review identity, endpoint, and network events for "+a.getAsset()+"; preserve relevant logs.","Confirm context with the asset owner before closing. AI output is advisory only.");
  }

  @PostMapping("/logs/upload")
  public ResponseEntity<?> uploadLogFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
    }
    String filename = file.getOriginalFilename();
    if (filename == null || (!filename.endsWith(".txt") && !filename.endsWith(".csv") && !filename.endsWith(".json"))) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type. Only .txt, .csv, and .json are allowed."));
    }
    try {
      String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
      LogFile logFile = logAnalysisService.processLogFile(filename, file.getSize(), content);
      return ResponseEntity.ok(logFile);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to process log file: " + e.getMessage()));
    }
  }

  @GetMapping("/logs/uploads")
  public List<LogFile> getUploadHistory() {
    return logFiles.findAllByOrderByUploadTimeDesc();
  }

  private Alert findAlert(Long id){
    return alerts.findById(id).orElseThrow(()->new NoSuchElementException("Alert not found: "+id));
  }

  private void apply(Alert a,AlertRequest r){
    a.setTitle(r.title());
    a.setAsset(r.asset());
    a.setDescription(r.description());
    a.setSeverity(r.severity());
    a.setPriorityScore(r.priorityScore());
    a.setRiskLevel(RiskLevel.from(r.priorityScore(), r.severity()));
    a.setAlertType(r.alertType());
    a.setAttackType(r.attackType());
    a.setLogDetails(r.logDetails());

    // Deriving/populating new fields
    String pri = "P4";
    if (r.severity() == Severity.CRITICAL) pri = "P1";
    else if (r.severity() == Severity.HIGH) pri = "P2";
    else if (r.severity() == Severity.MEDIUM) pri = "P3";
    a.setPriority(pri);

    // Extract source IP if available in log details, else default
    String ip = "127.0.0.1";
    if (r.logDetails() != null) {
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b").matcher(r.logDetails());
      if (m.find()) ip = m.group(1);
    }
    a.setSourceIP(ip);

    // Extract username if available, else default
    String user = "system";
    if (r.logDetails() != null) {
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\b(?:user|username|login|account|email)[:=]\\s*[\"']?([a-zA-Z0-9_\\-\\.@]+)[\"']?").matcher(r.logDetails());
      if (m.find()) user = m.group(1);
    }
    a.setUsername(user);

    a.setMatchedLogEntry(r.logDetails() != null ? r.logDetails() : r.description());

    // Recommendation based on attack type
    String rec = "Monitor account activity and review perimeter controls.";
    if (r.attackType() == AttackType.SQL_INJECTION) rec = "Enforce input sanitization and use prepared statements.";
    else if (r.attackType() == AttackType.PRIVILEGE_ESCALATION) rec = "Review authorization rules and disable root access where unnecessary.";
    else if (r.attackType() == AttackType.BRUTE_FORCE) rec = "Lock the account immediately and enable CAPTCHA/MFA.";
    else if (r.attackType() == AttackType.XSS) rec = "Sanitize web parameters and output HTML encoding.";
    else if (r.attackType() == AttackType.COMMAND_INJECTION) rec = "Block direct shell invocation and sanitize parameters.";
    else if (r.attackType() == AttackType.DIRECTORY_TRAVERSAL) rec = "Use absolute file mappings and restrict directory permissions.";
    else if (r.attackType() == AttackType.SUSPICIOUS_IP) rec = "Verify the traffic context and block the IP at the firewall level.";
    else if (r.attackType() == AttackType.UNAUTHORIZED_ACCESS) rec = "Review authentication credentials and endpoint authorization rules.";
    a.setRecommendation(rec);
  }

  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  Map<String,String> notFound(NoSuchElementException e){
    return Map.of("error",e.getMessage());
  }

  public record AlertRequest(@NotBlank String title,@NotBlank String asset,@NotBlank String description,@NotNull Severity severity,@NotNull @Min(0) @Max(100) Integer priorityScore,@NotNull AlertType alertType,@NotNull AttackType attackType,String logDetails){}
  public record StatusRequest(@NotNull AlertStatus status){}
  public record SeverityRequest(@NotNull Severity severity){}
  public record EscalationRequest(@NotBlank String reason){}
  public record InvestigationRequest(@NotBlank String notes, @NotBlank String actionsPerformed,
                                     @NotBlank String findings, @NotBlank String conclusion,
                                     String finalDecision){}
  public record NoteRequest(@NotNull Long alertId,@NotBlank String text,String analystName){}
  public record AlertResponse(Long id,String referenceCode,String title,String asset,String description,Severity severity,String status,RiskLevel riskLevel,AlertType category,AlertType alertType,AttackType attackType,Integer priorityScore,Integer riskScore,String logDetails,String logMessage,LocalDateTime createdAt,LocalDateTime timestamp,String priority,String sourceIP,String sourceIp,String username,String recommendation,String matchedLogEntry){
    static AlertResponse from(Alert a){
      return new AlertResponse(a.getId(),a.getReferenceCode(),a.getTitle(),a.getAsset(),a.getDescription(),a.getSeverity(),displayStatus(a.getStatus()),a.getRiskLevel(),categoryFor(a),a.getAlertType(),a.getAttackType(),a.getPriorityScore(),a.getPriorityScore(),a.getLogDetails(),a.getMatchedLogEntry(),a.getCreatedAt(),a.getCreatedAt(),a.getPriority(),a.getSourceIP(),a.getSourceIP(),a.getUsername(),a.getRecommendation(),a.getMatchedLogEntry());
    }
  }
  public record NoteResponse(Long id,Long alertId,String referenceCode,String alertTitle,String text,String analystName,LocalDateTime createdAt){
    static NoteResponse from(InvestigationNote n){
      return new NoteResponse(n.getId(),n.getAlert().getId(),n.getAlert().getReferenceCode(),n.getAlert().getTitle(),n.getText(),n.getAnalystName(),n.getCreatedAt());
    }
  }
  public record UserResponse(String username, List<String> roles){}
  public record InvestigationEventResponse(Long id, Long alertId, String alertReferenceCode, String eventType,
                                           String actorUsername, String actorRole, String assignedTo, String assignedRole,
                                           String reason, String oldStatus, String newStatus, String oldSeverity,
                                           String newSeverity, String notes, String actionsPerformed, String findings,
                                           String conclusion, String finalDecision, LocalDateTime occurredAt) {
    static InvestigationEventResponse from(InvestigationEvent event) {
      return new InvestigationEventResponse(event.getId(), event.getAlertId(), event.getAlertReferenceCode(),
          event.getEventType().name(), event.getActorUsername(), event.getActorRole(), event.getAssignedTo(),
          event.getAssignedRole(), event.getReason(), event.getOldStatus(), event.getNewStatus(),
          event.getOldSeverity(), event.getNewSeverity(), event.getNotes(), event.getActionsPerformed(),
          event.getFindings(), event.getConclusion(), event.getFinalDecision(), event.getOccurredAt());
    }
  }
  public record AiResponse(String confidence,String concern,String recommendedNextStep,String analystReminder){}
}
