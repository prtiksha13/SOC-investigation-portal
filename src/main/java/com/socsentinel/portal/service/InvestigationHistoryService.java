package com.socsentinel.portal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socsentinel.portal.model.Alert;
import com.socsentinel.portal.model.AlertStatus;
import com.socsentinel.portal.model.InvestigationEvent;
import com.socsentinel.portal.model.InvestigationEventType;
import com.socsentinel.portal.model.Severity;
import com.socsentinel.portal.repository.InvestigationEventRepository;

@Service
public class InvestigationHistoryService {
    private final InvestigationEventRepository repository;

    public InvestigationHistoryService(InvestigationEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InvestigationEvent record(Alert alert, InvestigationEventType type, String username, String role) {
        return repository.save(new InvestigationEvent(alert.getId(), alert.getReferenceCode(), type, username, role));
    }

    @Transactional(readOnly = true)
    public List<InvestigationEvent> timeline(Long alertId) {
        return repository.findAllByAlertIdOrderByOccurredAtAsc(alertId);
    }

    public InvestigationEvent statusChanged(Alert alert, String username, String role,
                                            AlertStatus oldStatus, AlertStatus newStatus) {
        InvestigationEvent event = record(alert, InvestigationEventType.STATUS_CHANGED, username, role);
        event.setOldStatus(oldStatus == null ? null : oldStatus.name());
        event.setNewStatus(newStatus == null ? null : newStatus.name());
        return repository.save(event);
    }

    public InvestigationEvent severityChanged(Alert alert, String username, String role,
                                              Severity oldSeverity, Severity newSeverity) {
        InvestigationEvent event = record(alert, InvestigationEventType.SEVERITY_CHANGED, username, role);
        event.setOldSeverity(oldSeverity == null ? null : oldSeverity.name());
        event.setNewSeverity(newSeverity == null ? null : newSeverity.name());
        return repository.save(event);
    }

    public InvestigationEvent investigationSaved(Alert alert, String username, String role,
                                                String notes, String actions, String findings,
                                                String conclusion, String finalDecision) {
        InvestigationEvent event = record(alert, InvestigationEventType.INVESTIGATION_SAVED, username, role);
        event.setNotes(notes);
        event.setActionsPerformed(actions);
        event.setFindings(findings);
        event.setConclusion(conclusion);
        event.setFinalDecision(finalDecision);
        event.setNewStatus(alert.getStatus().name());
        return repository.save(event);
    }

    public InvestigationEvent noteAdded(Alert alert, String username, String role, String notes) {
        InvestigationEvent event = record(alert, InvestigationEventType.NOTE_ADDED, username, role);
        event.setNotes(notes);
        event.setNewStatus(alert.getStatus().name());
        return repository.save(event);
    }

    public InvestigationEvent escalated(Alert alert, String username, String role, String reason) {
        InvestigationEvent event = record(alert, InvestigationEventType.ESCALATED, username, role);
        event.setReason(reason);
        event.setNewStatus(alert.getStatus().name());
        return repository.save(event);
    }

    public InvestigationEvent assigned(Alert alert, String username, String role,
                                       String assignedTo, String assignedRole) {
        InvestigationEvent event = record(alert, InvestigationEventType.ASSIGNED, username, role);
        event.setAssignedTo(assignedTo);
        event.setAssignedRole(assignedRole);
        return repository.save(event);
    }
}
