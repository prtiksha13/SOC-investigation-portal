package com.socsentinel.portal.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class AlertSchemaMigration {
    private static final Logger log = LoggerFactory.getLogger(AlertSchemaMigration.class);
    private final DataSource dataSource;

    public AlertSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void expandAlertEnums() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (!metadata.getDatabaseProductName().toLowerCase().contains("mysql")) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE alerts MODIFY COLUMN status ENUM('OPEN','INVESTIGATING','UNDER_INVESTIGATION','RESOLVED','CLOSED') NOT NULL");
                statement.executeUpdate("ALTER TABLE alerts MODIFY COLUMN alert_type ENUM('AUTHENTICATION','AUTHORIZATION','APPLICATION','NETWORK','SYSTEM','SECURITY','ENDPOINT','DATA_EXFILTRATION','POLICY_VIOLATION','OTHER') NOT NULL");
            }
        } catch (SQLException exception) {
            log.warn("Alert status/category schema migration could not be applied: {}", exception.getMessage());
        }
    }
}
