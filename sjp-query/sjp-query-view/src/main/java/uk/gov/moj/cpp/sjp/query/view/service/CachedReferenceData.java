package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.exception.ResultNotFoundException;
import uk.gov.moj.cpp.sjp.query.view.exception.WithdrawalReasonNotFoundException;

import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.collections.MapUtils;

public class CachedReferenceData {
    private ReferenceDataService referenceDataService;
    private Map<UUID, JsonObject> withdrawalReasonsByIds;
    private Map<String, JsonObject> resultIds;
    private JsonEnvelope envelope;

    public CachedReferenceData(final ReferenceDataService referenceDataService, final JsonEnvelope envelope) {
        this.referenceDataService = referenceDataService;
        this.envelope = envelope;
    }

    public String getWithdrawalReason(final UUID withdrawalReasonId) {
        if (MapUtils.isEmpty(withdrawalReasonsByIds)) {
            withdrawalReasonsByIds = referenceDataService.getWithdrawalReasons(envelope).stream()
                    .collect(toMap(withdrawalReason -> fromString(withdrawalReason.getString("id")), identity()));
        }
        return ofNullable(withdrawalReasonsByIds.get(withdrawalReasonId))
                .map(withdrawalReason -> withdrawalReason.getString("reasonCodeDescription"))
                .orElseThrow(() -> new WithdrawalReasonNotFoundException(withdrawalReasonId));
    }

    public UUID getResultId(final String resultCode) {
        if (MapUtils.isEmpty(resultIds)) {
            resultIds = referenceDataService.getResultIds(envelope).stream()
                    .collect(toMap(result -> result.getString("code"), identity()));
        }
        return ofNullable(resultIds.get(resultCode))
                .map(result -> result.getString("id"))
                .map(UUID::fromString)
                .orElseThrow(() -> new ResultNotFoundException(resultCode));
    }
}
