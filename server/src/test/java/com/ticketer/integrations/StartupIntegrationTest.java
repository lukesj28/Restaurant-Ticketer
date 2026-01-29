package com.ticketer.integrations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "tickets.dir=target/test-tickets/startup",
        "recovery.file=target/test-tickets/startup/recovery.json"
})
class StartupIntegrationTest {

    @Test
    void contextLoads() {
    }

}
