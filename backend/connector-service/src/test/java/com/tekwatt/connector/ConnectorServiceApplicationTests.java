package com.tekwatt.connector;

import com.tekwatt.connector.entity.Connector;
import com.tekwatt.connector.entity.ConnectorStatus;
import com.tekwatt.connector.entity.ConnectorType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConnectorServiceApplicationTests {
    @Test void contextLoads() {}

    @Test
    void newlyProvisionedConnectorIsAvailableForCharging() {
        Connector connector = new Connector(
                UUID.randomUUID(), UUID.randomUUID(), 1, ConnectorType.CCS2,
                new BigDecimal("60.00"), 800, 200);

        assertThat(connector.getStatus()).isEqualTo(ConnectorStatus.AVAILABLE);
    }
}
