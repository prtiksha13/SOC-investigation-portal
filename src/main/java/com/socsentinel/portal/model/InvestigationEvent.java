package com.socsentinel.portal.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "alert_investigation_history")
public class InvestigationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long alertId;

    private String alertReferenceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestigationEventType eventType;

    @Column(nullable = false)
    private String actorUsername;

    private String actorRole;
    private String assignedTo;
    private String assignedRole;
    private String reason;
    private String oldStatus;
    private String newStatus;
    private String oldSeverity;
    private String newSeverity;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String actionsPerformed;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String findings;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String conclusion;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String finalDecision;

    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    protected InvestigationEvent() {
    }

    public InvestigationEvent(Long alertId, String alertReferenceCode, InvestigationEventType eventType,
                              String actorUsername, String actorRole) {
        this.alertId = alertId;
        this.alertReferenceCode = alertReferenceCode;
        this.eventType = eventType;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
    }

    public Long getId() { return id; }
    public Long getAlertId() { return alertId; }
    public String getAlertReferenceCode() { return alertReferenceCode; }
    public InvestigationEventType getEventType() { return eventType; }
    public String getActorUsername() { return actorUsername; }
    public String getActorRole() { return actorRole; }
    public String getAssignedTo() { return assignedTo; }
    public String getAssignedRole() { return assignedRole; }
    public String getReason() { return reason; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getOldSeverity() { return oldSeverity; }
    public String getNewSeverity() { return newSeverity; }
    public String getNotes() { return notes; }
    public String getActionsPerformed() { return actionsPerformed; }
    public String getFindings() { return findings; }
    public String getConclusion() { return conclusion; }
    public String getFinalDecision() { return finalDecision; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public void setAssignedRole(String assignedRole) { this.assignedRole = assignedRole; }
    public void setReason(String reason) { this.reason = reason; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public void setOldSeverity(String oldSeverity) { this.oldSeverity = oldSeverity; }
    public void setNewSeverity(String newSeverity) { this.newSeverity = newSeverity; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setActionsPerformed(String actionsPerformed) { this.actionsPerformed = actionsPerformed; }
    public void setFindings(String findings) { this.findings = findings; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public void setFinalDecision(String finalDecision) { this.finalDecision = finalDecision; }
}
