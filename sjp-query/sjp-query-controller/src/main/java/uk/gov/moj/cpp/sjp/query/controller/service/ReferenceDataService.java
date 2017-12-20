package uk.gov.moj.cpp.sjp.query.controller.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_CONTROLLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.Range;

class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_CONTROLLER)
    private Requester requester;

    String resolveOffenceTitle(final JsonEnvelope envelope, final String offenceCode, final String date) {
        final JsonArray offences = requestOffences(envelope, offenceCode)
                .getJsonArray("offences");
        return findOffence(offences, offenceCode, date)
                .map(offence -> offence.getString("title"))
                .orElse(null);
    }

    private Optional<JsonObject> findOffence(final JsonArray offences, final String offenceCode, final String date) {
        return offences
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(offence -> offenceCode.equals(offence.getString("cjsoffencecode")))
                .filter(offence -> {
                    final LocalDate startDate = LocalDate.parse(offence.getString("offencestartdate"));
                    Range<LocalDate> offenceDateRange;
                    if (offence.containsKey("offenceenddate")) {
                        final LocalDate endDate = LocalDate.parse(offence.getString("offenceenddate"));
                        offenceDateRange = Range.closed(startDate, endDate);
                    } else {
                        offenceDateRange = Range.atLeast(startDate);
                    }
                    return offenceDateRange.contains(LocalDate.parse(date));
                })
                .findFirst();
    }

    private JsonObject requestOffences(final JsonEnvelope envelope, final String offenceCode) {
        final JsonEnvelope request = enveloper
                .withMetadataFrom(envelope, "referencedata.query.offences")
                .apply(Json.createObjectBuilder().add("cjsoffencecode", offenceCode).build());
        final JsonEnvelope response = requester.request(request);
        return response.payloadAsJsonObject();
    }
}
