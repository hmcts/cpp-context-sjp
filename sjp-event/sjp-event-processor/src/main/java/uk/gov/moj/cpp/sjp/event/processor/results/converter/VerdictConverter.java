package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.core.courts.Verdict.verdict;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;

public class VerdictConverter {

    public static final Map<String, String> verdictTypeMap = ImmutableMap.of(
            "NO_VERDICT", "None",
            "FOUND_GUILTY", "G",
            "FOUND_NOT_GUILTY", "N",
            "PROVED_SJP", "PSJ");

    private final JCachedReferenceData jCachedReferenceData;

    @Inject
    public VerdictConverter(final JCachedReferenceData jCachedReferenceData) {
        this.jCachedReferenceData = jCachedReferenceData;
    }

    public Verdict getVerdict(final ConvictionInfo convictionInfo) {
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataBuilder()
                .withId(UUID.randomUUID())
                .withName("referencedata.query.verdict-types")
                .build(), NULL);

        final VerdictType verdictType = convictionInfo.getVerdictType();
        final UUID offenceId = convictionInfo.getOffenceId();
        final LocalDate convictionDate = convictionInfo.getConvictionDate();

        if (Objects.nonNull(verdictType)
                && !verdictType.equals(VerdictType.NO_VERDICT)) {
            final Optional<JsonObject> verdictOptional = jCachedReferenceData.getVerdictForMagistrate(verdictTypeMap.get(verdictType.name()), emptyEnvelope);
            if (verdictOptional.isPresent()) {
                final JsonObject verdictObject = verdictOptional.get();
                final String id = verdictObject.getString("id");
                return verdict()
                        .withVerdictDate(convictionDate != null ? convictionDate.toString() : null)
                        .withOffenceId(offenceId)
                        .withVerdictType(uk.gov.justice.core.courts.VerdictType.verdictType()
                                .withId(UUID.fromString(id))
                                .withCategory(verdictObject.getString("category"))
                                .withCategoryType(verdictObject.getString("categoryType"))
                                .withVerdictCode(verdictObject.getString("verdictCode"))
                                .withCjsVerdictCode(verdictObject.getString("cjsVerdictCode"))
                                .build()
                        ).build();
            }
        }
        return null;
    }
}
