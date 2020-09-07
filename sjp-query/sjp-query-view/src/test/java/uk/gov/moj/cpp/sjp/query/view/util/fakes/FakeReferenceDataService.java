package uk.gov.moj.cpp.sjp.query.view.util.fakes;

import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.converter.ResultCode;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class FakeReferenceDataService extends ReferenceDataService {

    private List<JsonObject> withdrawalReasons = new ArrayList<>();
    private Map<String, JsonObject> enforcementAreaByPostcode = new HashMap<>();
    private Map<String, JsonObject> enforcementAreaByCourtCode = new HashMap<>();

    @Override
    public List<JsonObject> getWithdrawalReasons(final JsonEnvelope jsonEnvelope) {
        return withdrawalReasons;
    }

    @Override
    public List<JsonObject> getResultIds(final JsonEnvelope envelope) {
        final List<JsonObject> resultIds = Arrays.stream(ResultCode.values())
                .map(this::toJsonResult)
                .collect(toList());

        return resultIds;
    }

    @Override
    public Optional<JsonObject> getEnforcementAreaByPostcode(final String postcode, final JsonEnvelope sourceEvent) {
        return Optional.ofNullable(this.enforcementAreaByPostcode.getOrDefault(postcode, null));
    }

    @Override
    public Optional<JsonObject> getEnforcementAreaByLocalJusticeAreaNationalCourtCode(final String courtCode, final JsonEnvelope sourceEvent) {
        return Optional.ofNullable(this.enforcementAreaByCourtCode.getOrDefault(courtCode, null));
    }

    public void addWithdrawalReason(final UUID id, final String reason) {
        final JsonObject withdrawReason = createObjectBuilder()
                .add("id", id.toString())
                .add("reasonCodeDescription", reason)
                .build();
        withdrawalReasons.add(withdrawReason);
    }

    public void addEnforcementAreaByPostcode(final String postcode, final JsonObject enforcementArea) {
        this.enforcementAreaByPostcode.put(postcode, enforcementArea);
    }

    public void addEnforcementAreaByLocalJusticeAreaNationalCourtCode(final String courtCode, final JsonObject enforcementArea) {
        this.enforcementAreaByCourtCode.put(courtCode, enforcementArea);
    }

    private JsonObject toJsonResult(final ResultCode resultCode) {
        return createObjectBuilder()
                .add("id", resultCode.getResultDefinitionId().toString())
                .add("code", resultCode.toString())
                .build();
    }
}
