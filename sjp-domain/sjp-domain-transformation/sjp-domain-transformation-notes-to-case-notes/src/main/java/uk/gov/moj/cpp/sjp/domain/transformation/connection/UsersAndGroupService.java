package uk.gov.moj.cpp.sjp.domain.transformation.connection;

import uk.gov.moj.cpp.sjp.domain.transformation.notes.UserDetails;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@SuppressWarnings({"squid:S2221", "squid:S00112"})
public class UsersAndGroupService {

    private static final String USERS_GROUPS_DS_VIEW_STORE = "java:/app/usersgroups/DS.viewstore";

    private static final String GET_USER_DETAILS = "select first_name, last_name from cpp_user where id = CAST(? as uuid)";

    private ConnectionProvider connectionProvider = new ConnectionProvider(USERS_GROUPS_DS_VIEW_STORE);

    // synchronized prevents running out of database connections
    public synchronized UserDetails getUserDetails(final String userId) {
        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement statement = connection.prepareStatement(GET_USER_DETAILS)) {

            statement.setString(1, userId);
            return buildUserDetails(statement);

        } catch (Exception e) {
            throw new RuntimeException("Error retrieving data ", e);
        }
    }

    private UserDetails buildUserDetails(final PreparedStatement statement) {
        try (final ResultSet rs = statement.executeQuery()) {
            rs.next();
            final String firstName = rs.getString("first_name");
            final String lastName = rs.getString("last_name");
            return new UserDetails(firstName, lastName);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving data from result set", e);
        }
    }

}
