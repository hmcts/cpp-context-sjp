package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.connection;

import static java.lang.String.format;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@SuppressWarnings({"squid:S2221", "squid:S00112"})
public class ConnectionProvider {

    private final String datasourceJndi;
    private Connection connection;

    public ConnectionProvider(final String datasourceJndi) {
        this.datasourceJndi = datasourceJndi;
    }

    public Connection getConnection() {
        try {
            if (null == connection || connection.isClosed()) {
                final InitialContext context = new InitialContext();
                final DataSource dataSource = (DataSource) context.lookup(datasourceJndi);
                connection = dataSource.getConnection();
            }
        } catch (NamingException e) {
            throw new RuntimeException(format("JNDI lookup error: %s", e));
        } catch (SQLException e) {
            throw new RuntimeException(format("SQLException: %s", e));
        }
        return connection;
    }

}
