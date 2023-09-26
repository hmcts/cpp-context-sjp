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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerdictConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerdictConverter.class);
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
        return getVerdict(convictionInfo.getVerdictType(), convictionInfo.getOffenceId(), convictionInfo.getConvictionDate());
    }

    public Verdict getVerdict(final VerdictType verdictType, final UUID offenceId, final LocalDate convictionDate) {
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataBuilder()
                .withId(UUID.randomUUID())
                .withName("referencedata.query.verdict-types-jurisdiction")
                .build(), NULL);

        if (Objects.nonNull(verdictType)
                && !verdictType.equals(VerdictType.NO_VERDICT)) {
            final Optional<JsonObject> verdictOptional = jCachedReferenceData.getVerdict(verdictTypeMap.get(verdictType.name()), emptyEnvelope);
            if (verdictOptional.isPresent()) {
                final JsonObject verdictObject = verdictOptional.get();
                LOGGER.info("verdict from reference data : {}",verdictObject);
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
