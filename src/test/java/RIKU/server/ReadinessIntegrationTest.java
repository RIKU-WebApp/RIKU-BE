package RIKU.server;

import com.amazonaws.services.s3.AmazonS3;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0", "management.server.address=127.0.0.1",
        "management.endpoints.web.exposure.include=health", "management.endpoint.health.show-details=never",
        "management.endpoint.health.probes.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:readiness-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "management.endpoint.health.group.readiness.include=readinessState,db"
})
class ReadinessIntegrationTest {
    @MockBean AmazonS3 amazonS3;
    @Autowired TestRestTemplate http;
    @Autowired ApplicationContext context;
    @Autowired DataSource dataSource;
    @LocalServerPort int appPort;
    @Value("${local.management.port}") int managementPort;

    @Test
    void readinessIsSeparateFromPublicPortAndFailsWhenApplicationRefusesTraffic() {
        String path = "/actuator/health/readiness";
        var ready = http.getForEntity("http://127.0.0.1:" + managementPort + path, String.class);
        assertThat(ready.getStatusCode().value()).isEqualTo(200);
        assertThat(ready.getBody()).isEqualTo("{\"status\":\"UP\"}");
        assertThat(http.getForEntity("http://127.0.0.1:" + appPort + path, String.class)
                .getStatusCode().is2xxSuccessful()).isFalse();
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        try {
            assertThat(http.getForEntity("http://127.0.0.1:" + managementPort + path, String.class)
                    .getStatusCode().value()).isEqualTo(503);
        } finally {
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
        ((HikariDataSource) dataSource).close();
        var databaseDown = http.getForEntity("http://127.0.0.1:" + managementPort + path, String.class);
        assertThat(databaseDown.getStatusCode().value()).isEqualTo(503);
        assertThat(databaseDown.getBody()).isEqualTo("{\"status\":\"DOWN\"}");
    }
}
