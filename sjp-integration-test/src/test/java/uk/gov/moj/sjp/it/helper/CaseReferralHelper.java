package uk.gov.moj.sjp.it.helper;

import static java.util.Optional.empty;
import static java.util.Optional.of;

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

    public static Optional<CaseCourtReferralStatus> findReferralStatusForCase(UUID caseId) {

        try (final Connection connection = connectionProvider.getViewStoreConnection("sjp");
             final PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_CASE_REFERRAL_STATUS_FOR_CASE)) {

            preparedStatement.setObject(1, caseId);

            final ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {

                final ZonedDateTime receivedAt = ZonedDateTimes.fromSqlTimestamp(resultSet.getTimestamp("requested_at"));
                final String rejectionReason = resultSet.getString("rejection_reason");
                final ZonedDateTime rejectedAt = Optional.ofNullable(resultSet.getTimestamp("rejected_at"))
                        .map(ZonedDateTimes::fromSqlTimestamp)
                        .orElse(null);

                return of(new CaseCourtReferralStatus(caseId, receivedAt, rejectedAt, rejectionReason));
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to lookup CaseCourtReferralStatus", e);
        }

        return empty();
    }
}
