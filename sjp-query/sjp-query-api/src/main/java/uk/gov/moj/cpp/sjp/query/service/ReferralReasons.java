package uk.gov.moj.cpp.sjp.query.service;


import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.collections.MapUtils;


public class ReferralReasons {
    private ReferenceDataService referenceDataService;
    private Map<UUID, JsonObject> referralReasonsByIds;
    private JsonEnvelope envelope;

    public ReferralReasons(final ReferenceDataService referenceDataService, final JsonEnvelope envelope) {
        this.referenceDataService = referenceDataService;
        this.envelope = envelope;
    }

    public Optional<JsonObject> getReferralReason(final UUID referralReasonId) {
        if (MapUtils.isEmpty(referralReasonsByIds)) {
            referralReasonsByIds = referenceDataService.getReferralReasons(envelope).stream()
                    .collect(toMap(referralReason -> UUID.fromString(referralReason.getString("id")), identity()));
        }
        return ofNullable(referralReasonsByIds.get(referralReasonId));
    }
}
