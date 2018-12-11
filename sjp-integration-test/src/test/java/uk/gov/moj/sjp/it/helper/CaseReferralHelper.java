package uk.gov.moj.sjp.it.helper;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

public final class CaseReferralHelper {

    private static final String SQL_FIND_CASE_REFERRAL_STATUS_FOR_CASE =
            "SELECT * from case_court_referral_status WHERE case_id = ?";

    private static final TestJdbcConnectionProvider connectionProvider = new TestJdbcConnectionProvider();

    public static CaseCourtReferralStatus findReferralStatusForCase(UUID caseId) {
        try (final Connection connection = connectionProvider.getViewStoreConnection("sjp");
             final PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_CASE_REFERRAL_STATUS_FOR_CASE)) {

            preparedStatement.setObject(1, caseId);

            return fetchReferralStatusForCase(preparedStatement, caseId);
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to lookup CaseCourtReferralStatus", e);
        }
    }

    private static CaseCourtReferralStatus fetchReferralStatusForCase(
            PreparedStatement preparedStatement,
            UUID caseId) throws SQLException {

        final ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {

            final ZonedDateTime receivedAt = ZonedDateTimes.fromSqlTimestamp(resultSet.getTimestamp("requested_at"));
            final String rejectionReason = resultSet.getString("rejection_reason");
            final ZonedDateTime rejectedAt = Optional.ofNullable(resultSet.getTimestamp("rejected_at"))
                    .map(ZonedDateTimes::fromSqlTimestamp)
                    .orElse(null);
            final String urn = resultSet.getString("urn");

            return new CaseCourtReferralStatus(caseId, urn, receivedAt, rejectedAt, rejectionReason);
        }

        return null;
    }
}
