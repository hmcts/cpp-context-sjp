package uk.gov.moj.cpp.sjp.query.service;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.collections.MapUtils;

public class WithdrawalReasons {
    private ReferenceDataService referenceDataService;
    private Map<UUID, JsonObject> withdrawalReasonsByIds;
    private JsonEnvelope envelope;

    public WithdrawalReasons(final ReferenceDataService referenceDataService, final JsonEnvelope envelope) {
        this.referenceDataService = referenceDataService;
        this.envelope = envelope;
    }

    public Optional<String> getWithdrawalReason(final UUID withdrawalReasonId) {
        if (MapUtils.isEmpty(withdrawalReasonsByIds)) {
            withdrawalReasonsByIds = referenceDataService.getWithdrawalReasons(envelope).stream()
                    .collect(toMap(withdrawalReason -> fromString(withdrawalReason.getString("id")), identity()));
        }
        return ofNullable(withdrawalReasonsByIds.get(withdrawalReasonId)).map(withdrawalReason -> withdrawalReason.getString("reasonCodeDescription"));
    }
}
