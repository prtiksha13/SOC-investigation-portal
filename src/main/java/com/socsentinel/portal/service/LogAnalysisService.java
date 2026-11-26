package com.socsentinel.portal.service;

import com.socsentinel.portal.model.*;
import com.socsentinel.portal.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

@Service
public class LogAnalysisService {

    private final AlertRepository alertRepository;
    private final LogFileRepository logFileRepository;

    public LogAnalysisService(AlertRepository alertRepository, LogFileRepository logFileRepository) {
        this.alertRepository = alertRepository;
        this.logFileRepository = logFileRepository;
    }

    public LogFile processLogFile(String filename, long fileSize, String content) {
        List<String> lines = new ArrayList<>();

        // Check if it's a JSON array
        String trimmed = content.trim();
        if (trimmed.startsWith("[")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, Object>> list = mapper.readValue(trimmed, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> map : list) {
                    StringBuilder sb = new StringBuilder();
                    map.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
                    lines.add(sb.toString().trim());
                }
            } catch (Exception e) {
                // Fallback to text line split if JSON parse fails
                lines = Arrays.asList(content.split("\\r?\\n"));
            }
        } else {
            // Plain text or CSV line split
            lines = Arrays.asList(content.split("\\r?\\n"));
        }

        int totalLines = lines.size();
        List<Alert> detectedAlerts = new ArrayList<>();
        Map<String, Integer> failedLoginsByIp = new HashMap<>();

        // Regex for IP address
        Pattern ipPattern = Pattern.compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b");
        // Regex for Username
        Pattern userPattern = Pattern.compile("(?i)\\b(?:user|username|login|account|email)[:=]\\s*[\"']?([a-zA-Z0-9_\\-\\.@]+)[\"']?");

        for (String rawLine : lines) {
            if (rawLine.trim().isEmpty()) continue;

            // Extract IP
            String sourceIP = "127.0.0.1";
            Matcher ipMatcher = ipPattern.matcher(rawLine);
            if (ipMatcher.find()) {
                sourceIP = ipMatcher.group(1);
            }

            // Extract Username
            String username = "system";
            Matcher userMatcher = userPattern.matcher(rawLine);
            if (userMatcher.find()) {
                username = userMatcher.group(1);
            }

            String lower = rawLine.toLowerCase();
            AttackType attackType = null;
            String title = "";
            Severity severity = Severity.LOW;
            String priority = "P4";
            int score = 25;
            AlertType alertType = AlertType.OTHER;
            String desc = "";
            String rec = "";

            // 1. SQL Injection
            if ((lower.contains("select ") && (lower.contains("from ") || lower.contains("where "))) ||
                lower.contains("union select") || lower.contains("or 1=1") || lower.contains("' or '1'='1") ||
                lower.contains("--") || lower.contains("/*") || lower.contains("sleep(") || lower.contains("pg_sleep")) {
                attackType = AttackType.SQL_INJECTION;
                title = "SQL Injection Attempt";
                severity = Severity.CRITICAL;
                priority = "P1";
                score = 95;
                alertType = AlertType.APPLICATION;
                desc = "SQL Injection signatures detected in the logs.";
                rec = "Enforce input sanitization and use prepared statements.";
            }
            // 2. Privilege Escalation
            else if (lower.contains("privilege escalation") || lower.contains("escalate privilege") || 
                     lower.contains("got root") || lower.contains("sudo ") || lower.contains("runas ") || 
                     lower.contains("administrator privilege")) {
                attackType = AttackType.PRIVILEGE_ESCALATION;
                title = "Privilege Escalation Attempt";
                severity = Severity.CRITICAL;
                priority = "P1";
                score = 95;
                alertType = AlertType.AUTHORIZATION;
                desc = "Unauthorized privilege escalation signatures detected.";
                rec = "Review authorization rules and disable root access where unnecessary.";
            }
            // 3. Explicit Brute Force
            else if (lower.contains("brute force") || lower.contains("brute_force") || lower.contains("credential stuffing")) {
                attackType = AttackType.BRUTE_FORCE;
                title = "Brute Force Attack";
                severity = Severity.HIGH;
                priority = "P2";
                score = 75;
                alertType = AlertType.SECURITY;
                desc = "Explicit brute force signatures found in the log entry.";
                rec = "Lock the account immediately and enable CAPTCHA/MFA.";
            }
            // 4. Successful login after repeated failures
            else if ((lower.contains("successful login") || lower.contains("login successful") || lower.contains("authentication successful"))
                    && failedLoginsByIp.getOrDefault(sourceIP, 0) >= 3) {
                attackType = AttackType.BRUTE_FORCE;
                title = "Successful Login After Multiple Failures";
                severity = Severity.CRITICAL;
                priority = "P1";
                score = 95;
                alertType = AlertType.AUTHENTICATION;
                desc = "A successful login followed multiple failed attempts from IP: " + sourceIP;
                rec = "Contain the source, reset credentials, and review the authenticated session.";
            }
            // 5. XSS
            else if (lower.contains("<script>") || lower.contains("javascript:") || lower.contains("onerror=") || 
                     lower.contains("onload=") || lower.contains("alert(") || lower.contains("<svg") || lower.contains("<img ")) {
                attackType = AttackType.XSS;
                title = "XSS Attempt";
                severity = Severity.HIGH;
                priority = "P2";
                score = 75;
                alertType = AlertType.APPLICATION;
                desc = "Cross-Site Scripting (XSS) input payload detected.";
                rec = "Sanitize web parameters and output HTML encoding.";
            }
            // 6. Port scan
            else if (lower.contains("port scan") || lower.contains("portscan") || lower.contains("syn scan") ||
                     lower.contains("multiple ports") || lower.contains("scanned ports")) {
                attackType = AttackType.PORT_SCAN;
                title = "Port Scan Detected";
                severity = Severity.HIGH;
                priority = "P2";
                score = 75;
                alertType = AlertType.NETWORK;
                desc = "Network reconnaissance or port scanning activity detected.";
                rec = "Review the source host and block unauthorized reconnaissance traffic.";
            }
            // 7. Suspicious PowerShell or process execution
            else if (lower.contains("powershell") || lower.contains("encodedcommand") || lower.contains("-enc ")) {
                attackType = AttackType.COMMAND_INJECTION;
                title = "Suspicious PowerShell Execution";
                severity = Severity.HIGH;
                priority = "P2";
                score = 78;
                alertType = AlertType.SYSTEM;
                desc = "PowerShell execution or encoded command activity detected.";
                rec = "Capture the command line, isolate the host, and validate the parent process.";
            }
            else if (lower.contains("malware") || lower.contains("malicious process") || lower.contains("suspicious process") ||
                     lower.contains("trojan") || lower.contains("ransomware")) {
                attackType = AttackType.MALWARE;
                title = "Malware or Suspicious Process Detected";
                severity = Severity.CRITICAL;
                priority = "P1";
                score = 95;
                alertType = AlertType.SECURITY;
                desc = "Malware or suspicious process indicators detected in the log entry.";
                rec = "Isolate the endpoint and preserve the process and filesystem evidence.";
            }
            // 8. SSH attack
            else if (lower.contains("ssh") && (lower.contains("failed") || lower.contains("invalid") ||
                     lower.contains("brute") || lower.contains("attack") || lower.contains("authentication"))) {
                attackType = AttackType.SSH_ATTACK;
                title = "SSH Attack Detected";
                severity = Severity.HIGH;
                priority = "P2";
                score = 75;
                alertType = AlertType.NETWORK;
                desc = "Suspicious SSH authentication activity detected.";
                rec = "Restrict SSH exposure, enforce key-based access, and review source activity.";
            }
            // 9. Command Injection
            else if (lower.contains("cmd.exe") || lower.contains("/bin/sh") || lower.contains("/bin/bash") || 
                     lower.contains("; wget") || lower.contains("; curl") || 
                     lower.contains("eval(") || lower.contains("exec(")) {
                attackType = AttackType.COMMAND_INJECTION;
                title = "OS Command Injection Attempt";
                severity = Severity.MEDIUM;
                priority = "P3";
                score = 55;
                alertType = AlertType.APPLICATION;
                desc = "Shell command execution invocation detected.";
                rec = "Block direct shell invocation and sanitize parameters.";
            }
            // 10. Directory Traversal
            else if (lower.contains("../") || lower.contains("..%2f") || lower.contains("..\\") || 
                     lower.contains("/etc/passwd") || lower.contains("win.ini") || lower.contains("boot.ini")) {
                attackType = AttackType.DIRECTORY_TRAVERSAL;
                title = "Directory Traversal Attempt";
                severity = Severity.MEDIUM;
                priority = "P3";
                score = 55;
                alertType = AlertType.APPLICATION;
                desc = "Path traversal sequences detected in parameters.";
                rec = "Use absolute file mappings and restrict directory permissions.";
            }
            // 11. Suspicious IP
            else if (lower.contains("suspicious ip") || lower.contains("malicious ip") || lower.contains("tor exit") || 
                     lower.contains("known scanner") || lower.contains("botnet") || 
                     lower.contains("198.51.100.23") || lower.contains("203.0.113.9") || lower.contains("185.220.101.5")) {
                attackType = AttackType.SUSPICIOUS_IP;
                title = "Suspicious IP Traffic";
                severity = Severity.MEDIUM;
                priority = "P3";
                score = 55;
                alertType = AlertType.NETWORK;
                desc = "Traffic from/to an IP address flagged in threat intelligence.";
                rec = "Verify the traffic context and block the IP at the firewall level.";
            }
            // 12. Unauthorized Access
            else if (lower.contains("403 forbidden") || lower.contains("401 unauthorized") || 
                     lower.contains("access denied") || lower.contains("permission denied") || lower.contains("unauthorized access")) {
                attackType = AttackType.UNAUTHORIZED_ACCESS;
                title = "Unauthorized Access Attempt";
                severity = Severity.MEDIUM;
                priority = "P3";
                score = 55;
                alertType = AlertType.AUTHORIZATION;
                desc = "Access denied or permission mismatch error logged.";
                rec = "Review authentication credentials and endpoint authorization rules.";
            }
            // 13. Failed Login
            else if (lower.contains("failed login") || lower.contains("login failed") || lower.contains("failed attempt") || 
                     lower.contains("authentication failed") || lower.contains("invalid password") || 
                     lower.contains("invalid credential") || lower.contains("unsuccessful login")) {

                int count = failedLoginsByIp.getOrDefault(sourceIP, 0) + 1;
                failedLoginsByIp.put(sourceIP, count);

                if (count >= 3) {
                    attackType = AttackType.BRUTE_FORCE;
                    title = "Brute Force Attack (Escalated)";
                    severity = Severity.HIGH;
                    priority = "P2";
                    score = 75;
                    alertType = AlertType.AUTHENTICATION;
                    desc = "Multiple failed login attempts (" + count + ") from IP: " + sourceIP;
                    rec = "Enforce temporary firewall IP block and account lockout.";
                } else {
                    attackType = AttackType.FAILED_LOGIN;
                    title = "Single Failed Login";
                    severity = Severity.LOW;
                    priority = "P4";
                    score = 25;
                    alertType = AlertType.AUTHENTICATION;
                    desc = "A single failed authentication event logged.";
                    rec = "Monitor the account for repeated sign-in failures.";
                }
            }

            if (attackType != null) {
                Alert a = new Alert();
                a.setTitle(title);
                a.setAsset("Log: " + filename);
                a.setDescription(desc);
                a.setSeverity(severity);
                a.setPriority(priority);
                a.setPriorityScore(score);
                a.setRiskLevel(RiskLevel.from(score, severity));
                a.setAlertType(alertType);
                a.setAttackType(attackType);
                a.setSourceIP(sourceIP);
                a.setUsername(username);
                a.setRecommendation(rec);
                a.setMatchedLogEntry(rawLine);
                a.setStatus(AlertStatus.OPEN);
                detectedAlerts.add(a);
            }
        }

        // Save alerts with unique references
        long baseCount = alertRepository.count();
        for (int i = 0; i < detectedAlerts.size(); i++) {
            Alert a = detectedAlerts.get(i);
            a.setReferenceCode("ALT-" + (2043 + baseCount + i + 1));
            alertRepository.save(a);
        }

        // Save log file meta
        LogFile lf = new LogFile();
        lf.setFileName(filename);
        lf.setFileSize(fileSize);
        lf.setTotalLines(totalLines);
        lf.setAlertCount(detectedAlerts.size());
        logFileRepository.save(lf);

        return lf;
    }
}
