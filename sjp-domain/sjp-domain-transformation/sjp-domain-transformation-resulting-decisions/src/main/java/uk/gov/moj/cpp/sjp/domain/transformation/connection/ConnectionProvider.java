package uk.gov.moj.cpp.sjp.domain.transformation.connection;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static java.lang.String.format;

@SuppressWarnings({"squid:S2221", "squid:S00112"})
public class ConnectionProvider {

    private final String datasourceJndi;

    public ConnectionProvider(final String datasourceJndi) {
        this.datasourceJndi = datasourceJndi;
    }

    public Connection getConnection() {
        try {
            final InitialContext context = new InitialContext();
            final DataSource dataSource = (DataSource) context.lookup(datasourceJndi);
            return dataSource.getConnection();
        } catch (NamingException e) {
            throw new RuntimeException(format("JNDI lookup error: %s", e));
        } catch (SQLException e) {
            throw new RuntimeException(format("SQLException: %s", e));
        }
    }

}
