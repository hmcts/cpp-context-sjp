package uk.gov.moj.sjp.it.helper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@SuppressWarnings("WeakerAccess")
public class DbHelper {

    private static final String GET_IDPC_PERMISSION_TEMPLATE = "SELECT name FROM structure_idpc_item WHERE suspect_id = '%s' AND allowed = %s";

    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public DbHelper() {
        String dbUrlProp = System.getProperty("dbUrl");
        String dbUserProp = System.getProperty("dbUser");
        String dbPasswordProp = System.getProperty("dbPassword");

        dbUrl = !isEmpty(dbUrlProp) ? dbUrlProp : "jdbc:postgresql://localhost:5432/structureviewstore";
        dbUser = !isEmpty(dbUserProp) ? dbUserProp : "structure";
        dbPassword = !isEmpty(dbPasswordProp) ? dbPasswordProp : "structure";

        try {
            if (dbUrl.contains("postgres")) {
                Class.forName("org.postgresql.Driver");
            } else {
                Class.forName("org.h2.Driver");
            }
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Error: " + cnfe.getMessage(), cnfe);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public List<String> findIdpcItems(UUID suspectId, boolean allowed) {
        String query = String.format(GET_IDPC_PERMISSION_TEMPLATE, suspectId, allowed);
        try (
                Connection connection = getViewStoreDbConnection();
                PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()
        ) {
            List<String> items = new ArrayList<>();
            while (resultSet.next()) {
                items.add(resultSet.getString("name"));
            }
            return items;
        } catch (SQLException e) {
            return emptyList();
        }
    }

    private Connection getViewStoreDbConnection() {
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
