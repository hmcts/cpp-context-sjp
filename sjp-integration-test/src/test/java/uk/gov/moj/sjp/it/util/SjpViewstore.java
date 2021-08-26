package uk.gov.moj.sjp.it.util;

import static java.util.Objects.nonNull;

import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification;
import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.UUID;

public class SjpViewstore {

    private TestJdbcConnectionProvider connectionProvider = new TestJdbcConnectionProvider();

    public SjpViewstore() {
    }

    public void insertNotificationOfEndorsementStatus(final UUID applicationDecisionId,
                                                      final UUID fileId,
                                                      final NotificationOfEndorsementStatus.Status status,
                                                      final ZonedDateTime updated) {
        String sql = "INSERT INTO notification_of_endorsement_status(application_decision_id, file_id, status, updated) " +
                "VALUES (?, ?, ?, ?);";

        try (final Connection connection = connectionProvider.getViewStoreConnection("sjp");
             final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, applicationDecisionId);
            preparedStatement.setObject(2, fileId);
            preparedStatement.setObject(3, status.name());
            preparedStatement.setObject(4, updated.toLocalDateTime());

            final int affectedRows = preparedStatement.executeUpdate();
            assert affectedRows > 0;
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to insert into notification_of_endorsement_status", e);
        }
    }

    public int countNotificationOfEndorsementStatus(final UUID applicationDecisionId,
                                                    final NotificationOfEndorsementStatus.Status status,
                                                    final UUID fileId) {
        return selectCount(applicationDecisionId, status, fileId);
    }

    public int countNotificationOfEndorsementStatus(final UUID applicationDecisionId,
                                                    final NotificationOfEndorsementStatus.Status status) {
        return selectCount(applicationDecisionId, status, null);
    }


    public int countNotificationOfEnforcementPendingApplicationEmails(final UUID applicationDecisionId,
                                                    final EnforcementNotification.Status status) {
        return selectCountOfEnforcementPendingApplicationEmails(applicationDecisionId, status, null);
    }

    private int selectCount(final UUID applicationDecisionId, final NotificationOfEndorsementStatus.Status status, final UUID fileId) {
        String sql = "SELECT count(*) " +
                "FROM notification_of_endorsement_status " +
                "WHERE application_decision_id = ? " +
                "AND status = ? ";

        if (nonNull(fileId)) {
            sql += "AND file_id = ? ";
        }

        int count = 0;

        try (final Connection connection = connectionProvider.getViewStoreConnection("sjp");
             final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, applicationDecisionId);
            preparedStatement.setObject(2, status.name());
            if (nonNull(fileId)) {
                preparedStatement.setObject(3, fileId);
            }

            final ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to query notification_of_endorsement_status", e);
        }
        return count;
    }


    public void insertNotificationOfEnforcementPendingStatus(final UUID applicationDecisionId,
                                                      final UUID fileId,
                                                      final EnforcementNotification.Status status,
                                                      final ZonedDateTime updated) {
        String sql = "INSERT INTO enforcement_notification(application_id, file_id, status, updated) " +
                "VALUES (?, ?, ?, ?);";

        try (final Connection connection = connectionProvider.getViewStoreConnection("sjp");
             final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, applicationDecisionId);
            preparedStatement.setObject(2, fileId);
            preparedStatement.setObject(3, status.name());
            preparedStatement.setObject(4, updated.toLocalDateTime());

            final int affectedRows = preparedStatement.executeUpdate();
            assert affectedRows > 0;
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to insert into enforcement_pending_application_notification_status", e);
        }
    }

    private int selectCountOfEnforcementPendingApplicationEmails(final UUID applicationId, final EnforcementNotification.Status status, final UUID fileId) {
        String sql = "SELECT count(*) FROM enforcement_notification WHERE application_id = ? AND status = ? ";

        if (nonNull(fileId)) {
            sql += "AND file_id = ? ";
        }

        int count = 0;

        try (final Connection connection = connectionProvider.getViewStoreConnection("sjp");
             final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, applicationId);
            preparedStatement.setObject(2, status.name());
            if (nonNull(fileId)) {
                preparedStatement.setObject(3, fileId);
            }

            final ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to query notification_of_endorsement_status", e);
        }
        return count;
    }
}