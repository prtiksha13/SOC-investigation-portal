package com.socsentinel.portal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String referenceCode;
    @NotBlank private String title;
    @NotBlank private String asset;
    @NotBlank @Column(length = 2000) private String description;
    @Enumerated(EnumType.STRING) @NotNull private Severity severity;
    @Enumerated(EnumType.STRING) @NotNull private AlertStatus status = AlertStatus.OPEN;
    @Enumerated(EnumType.STRING) @NotNull private RiskLevel riskLevel = RiskLevel.LOW;
    @Enumerated(EnumType.STRING) @NotNull private AlertType alertType = AlertType.OTHER;
    @Enumerated(EnumType.STRING) @NotNull private AttackType attackType = AttackType.UNKNOWN;
    @Min(0) @Max(100) private Integer priorityScore;
    @Column(length = 4000) private String logDetails;
    private String priority;
    private String sourceIP;
    private String username;
    @Column(length = 2000) private String recommendation;
    @Column(length = 4000) private String matchedLogEntry;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
    public Long getId(){return id;} public String getReferenceCode(){return referenceCode;} public void setReferenceCode(String v){referenceCode=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getAsset(){return asset;} public void setAsset(String v){asset=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;} public Severity getSeverity(){return severity;} public void setSeverity(Severity v){severity=v;}
    public AlertStatus getStatus(){return status;} public void setStatus(AlertStatus v){status=v;} public RiskLevel getRiskLevel(){return riskLevel;} public void setRiskLevel(RiskLevel v){riskLevel=v;}
    public AlertType getAlertType(){return alertType;} public void setAlertType(AlertType v){alertType=v;} public AttackType getAttackType(){return attackType;} public void setAttackType(AttackType v){attackType=v;}
    public Integer getPriorityScore(){return priorityScore;} public void setPriorityScore(Integer v){priorityScore=v;} public String getLogDetails(){return logDetails;} public void setLogDetails(String v){logDetails=v;}
    public String getPriority(){return priority;} public void setPriority(String v){priority=v;}
    public String getSourceIP(){return sourceIP;} public void setSourceIP(String v){sourceIP=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getRecommendation(){return recommendation;} public void setRecommendation(String v){recommendation=v;}
    public String getMatchedLogEntry(){return matchedLogEntry;} public void setMatchedLogEntry(String v){matchedLogEntry=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
