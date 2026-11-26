package com.socsentinel.portal;

import com.socsentinel.portal.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoAutoSeedAlertTest {

    @Autowired
    private AlertRepository alertRepository;

    @Test
    void appStartsWithoutSeededAlerts() {
        assertThat(alertRepository.count()).isZero();
    }
}
