package com.focushive.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class SimpleDatabaseTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertNotNull(dataSource);
    }

    @Test
    void canConnectToDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
        }
    }
}