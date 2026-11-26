package com.socsentinel.portal.model;

public enum RiskLevel {
    CRITICAL, HIGH, MEDIUM, LOW;

    public static RiskLevel from(int priorityScore, Severity severity) {
        if (priorityScore >= 90 || severity == Severity.CRITICAL) {
            return CRITICAL;
        }
        if (priorityScore >= 70 || severity == Severity.HIGH) {
            return HIGH;
        }
        if (priorityScore >= 40 || severity == Severity.MEDIUM) {
            return MEDIUM;
        }
        return LOW;
    }
}
