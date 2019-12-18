package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service;

import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.connection.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

public class IdMappingCache {
    public static final String OFFENCE_ID = "offenceId";
    private static final Logger LOGGER = getLogger(IdMappingCache.class);

    private static final String SJP_DS_VIEW_STORE = "java:/app/event-tool/DS.viewstore";
    private static final String GET_OFFENCE_ID_AND_DEFENDANT_ID =
            "select CAST(cd.id as text) as caseId, " +
                    "CAST(o.id as text) as offenceId, " +
                    "CAST(d.id as text) as defendantId " +
                    "from case_details cd, defendant d, offence o " +
                    "where cd.id = d.case_id " +
                    "and d.id = o.defendant_id";
    private static final String DEFENDANT_ID = "defendantId";

    private Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    private ConnectionProvider connectionProvider;

    private final ReentrantLock reentrantLock = new ReentrantLock();


    IdMappingCache() {
        this(new ConnectionProvider(SJP_DS_VIEW_STORE));
    }

    @VisibleForTesting
    public IdMappingCache(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    // for testability has to do this way otherwise static load would have been simple
    public String getId(final String caseId, final String type) {
        if (cache.size() == 0) {
            try {
                reentrantLock.lock();
                if (cache.size() == 0) {
                    cache = new ConcurrentHashMap<>();
                    initMap();
                }
            } finally {
                reentrantLock.unlock();
            }
        }

        String id = null;
        if (OFFENCE_ID.equals(type)) {
            id = cache.get(caseId).get(OFFENCE_ID);
        } else if (DEFENDANT_ID.equals(type)) {
            id = cache.get(caseId).get(DEFENDANT_ID);
        }

        if (id == null) {
            throw new TransformationException(type + " is null");
        }
        return id;
    }

    @SuppressWarnings("squid:S00112")
    private void initMap() {
        LOGGER.debug("Getting caseId-offenceId mapping from the view store");
        try (final ResultSet resultSet = connectionProvider.getConnection()
                .prepareStatement(GET_OFFENCE_ID_AND_DEFENDANT_ID).executeQuery()) {
            while (resultSet.next()) {
                final Map<String,String> map = new HashMap<>();
                map.put(OFFENCE_ID, resultSet.getString(OFFENCE_ID));
                map.put(DEFENDANT_ID, resultSet.getString(DEFENDANT_ID));
                cache.put(resultSet.getString("caseId"), map);
            }
        } catch (final SQLException e) {
            throw new TransformationException("Error retrieving caseId-offenceId mapping from the view store", e);
        }
        LOGGER.debug("Total Number of cases {}", cache.entrySet().size());
    }
}