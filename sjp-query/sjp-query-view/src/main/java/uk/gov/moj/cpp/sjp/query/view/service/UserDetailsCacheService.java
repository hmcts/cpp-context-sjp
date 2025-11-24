package uk.gov.moj.cpp.sjp.query.view.service;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDetailsCacheService {

    protected static final String UNRECOVERABLE_SYSTEM_ERROR = "unrecoverable system error";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsCacheService.class);
    private final ThreadLocal<JsonEnvelope> threadLocalContext = new ThreadLocal<>();

    @Inject
    UserAndGroupsService userAndGroupsService;

    private final LoadingCache<UUID, String> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .concurrencyLevel(20)
            .maximumSize(500)
            .build(new CacheLoader<UUID, String>() {
                @Override
                public String load(final UUID key) {
                    return loadUserDetails(key);
                }
            });

    String loadUserDetails(final UUID id) {
        return userAndGroupsService.getUserDetails(id, this.threadLocalContext.get());
    }

    @SuppressWarnings({"squid:S2139", "squid:S00112"})
    public String getUserName(final JsonEnvelope context, final UUID key) {
        this.threadLocalContext.set(context);
        try {
            return cache.get(key);
        } catch (ExecutionException executionException) {
            LOGGER.error("getUserDetails data service not available", executionException);
            throw new RuntimeException(UNRECOVERABLE_SYSTEM_ERROR, executionException);
        } finally {
            this.threadLocalContext.remove();
        }
    }

}
