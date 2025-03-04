package uk.gov.moj.sjp.it.util;

import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.justice.services.test.utils.common.host.TestHostProvider;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TestJdbcConnectionProvider {

    private static final String CONTEXT_NAME = "sjp";

    private static volatile TestJdbcConnectionProvider instance;
    private final HikariDataSource viewStoreDataSource;

    private TestJdbcConnectionProvider() {
        viewStoreDataSource = createDataSource();
    }

    public static TestJdbcConnectionProvider getInstance() {
        if (instance == null) {
            synchronized (TestJdbcConnectionProvider.class) {
                if (instance == null) {
                    instance = new TestJdbcConnectionProvider();
                }
            }
        }
        return instance;
    }

    private HikariDataSource createDataSource() {
        String host = TestHostProvider.getHost();
        String url = String.format("jdbc:postgresql://" + host + "/%sviewstore", CONTEXT_NAME);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(CONTEXT_NAME);
        config.setPassword(CONTEXT_NAME);
        config.setMaximumPoolSize(10);
        config.setIdleTimeout(30000);

        return new HikariDataSource(config);
    }

    public Connection getViewStoreConnection(final String contextName) {
        try {
            return viewStoreDataSource.getConnection();
        } catch (SQLException var8) {
            String message = String.format("Failed to get JDBC connection to %s View Store.", contextName);
            throw new DataAccessException(message, var8);
        }
    }
}


