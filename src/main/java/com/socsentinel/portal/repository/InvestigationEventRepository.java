package com.socsentinel.portal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socsentinel.portal.model.InvestigationEvent;

public interface InvestigationEventRepository extends JpaRepository<InvestigationEvent, Long> {
    List<InvestigationEvent> findAllByAlertIdOrderByOccurredAtAsc(Long alertId);
}
