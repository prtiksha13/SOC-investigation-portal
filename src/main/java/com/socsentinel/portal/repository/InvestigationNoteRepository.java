package com.socsentinel.portal.repository;
import com.socsentinel.portal.model.InvestigationNote; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface InvestigationNoteRepository extends JpaRepository<InvestigationNote, Long> { List<InvestigationNote> findAllByOrderByCreatedAtDesc(); }
