package com.socsentinel.portal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity @Table(name = "investigation_notes")
public class InvestigationNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "alert_id") private Alert alert;
    @NotBlank @Column(length = 3000) private String text;
    private String analystName = "SOC Analyst";
    private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId(){return id;} public Alert getAlert(){return alert;} public void setAlert(Alert v){alert=v;} public String getText(){return text;} public void setText(String v){text=v;}
    public String getAnalystName(){return analystName;} public void setAnalystName(String v){analystName=v;} public LocalDateTime getCreatedAt(){return createdAt;}
}
