package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceDataCachingService {

    private static final String KEY_PROSECUTORS = "PROSECUTORS";

    @Inject
    private ReferenceDataService referenceDataService;

    protected static final String UNRECOVERABLE_SYSTEM_ERROR = "unrecoverable system error";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataCachingService.class);

    private final LoadingCache<String, Object> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .concurrencyLevel(20)
            .maximumSize(500)
            .build(new CacheLoader<String, Object>() {
                @Override
                public Object load(final String key) {
                    if (KEY_PROSECUTORS.equals(key)) {
                        return loadAllProsecutors();
                    }
                    return null;
                }

                private Map<String, String> loadAllProsecutors() {
                    return referenceDataService.getAllProsecutors().map(allProsecutors -> allProsecutors.getValuesAs(JsonObject.class).stream()
                            .sorted(comparingInt(prosecutor -> prosecutor.getInt("sequenceNumber")))
                            .collect(toMap(prosecutorJson -> prosecutorJson.getString("shortName"),
                                    prosecutorJson -> prosecutorJson.getString("fullName"),
                                    (existingValue, newValue) -> newValue))
                    ).orElse(Collections.emptyMap());
                }
            });


    @SuppressWarnings({"squid:S2139", "squid:S00112"})
    public Map<String, String> getAllProsecutors() {
        try {
            return (Map<String, String>) cache.get(KEY_PROSECUTORS);
        } catch (final ExecutionException executionException) {
            LOGGER.error("getAllProsecutors data service not available", executionException);
            throw new RuntimeException(UNRECOVERABLE_SYSTEM_ERROR, executionException);
        }
    }
}
