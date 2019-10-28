package uk.gov.moj.cpp.sjp.domain.transformation.defendantdetailsmovedfrompeople;

import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

public class CaseIdDefendantIdCache {
    private static final Logger LOGGER = getLogger(CaseIdDefendantIdCache.class);
    private static CaseIdDefendantIdCache instance;

    private static final String SJP_DS_VIEW_STORE = "java:/app/event-tool/DS.viewstore";
    private static final String GET_DEFENDANT_ID_FOR_CASE_VS = "select case_id, id from defendant;";

    private static Map<String, String> map = new HashMap<>();
    private static ConnectionProvider connectionProvider = new ConnectionProvider(SJP_DS_VIEW_STORE);

    public static synchronized CaseIdDefendantIdCache getInstance() {
        if (instance == null) {
            instance = new CaseIdDefendantIdCache();
        }
        return instance;
    }

    private CaseIdDefendantIdCache() {
        initMap();
    }

    @SuppressWarnings("squid:S00112")
    private void initMap() {
        LOGGER.debug("Obtaining caseId-s and defendantId-s from viewstore");
        final long start = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        try (final ResultSet resultSet = connectionProvider.getConnection().prepareStatement(GET_DEFENDANT_ID_FOR_CASE_VS).executeQuery()) {
            while (resultSet.next()) {
                map.put(resultSet.getObject("case_id", UUID.class).toString(), resultSet.getObject("id", UUID.class).toString());
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Error retrieving data using prepared statement", e);
        }
        final long finish = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        LOGGER.debug("Building the caseId - defendantId map took {} seconds", finish - start);
        LOGGER.debug("Total Number of cases {}", map.entrySet().size());
    }

    public String getDefendantId(final String caseId) {
        return map.get(caseId);
    }
}
