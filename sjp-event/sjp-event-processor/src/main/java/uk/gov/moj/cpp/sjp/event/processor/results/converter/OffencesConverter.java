package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.annotations.VisibleForTesting;

public class OffencesConverter {

    private static final String MODE_OF_TRIAL = "1";
    public static final String TITLE = "title";

    @Inject
    private OffenceFactsConverter offenceFactsConverter;

    @Inject
    private PleaConverter pleaConverter;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Inject
    private VerdictConverter verdictConverter;

    public List<Offence> getOffences(final JsonObject sjpSessionPayload,
                                     final CaseDetails caseDetails,
                                     final DecisionAggregate resultsAggregate) {

        final List<Offence> offences = new ArrayList<>();

        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataBuilder()
                .withId(randomUUID())
                .withName("referencedataoffences.query.offences-list")
                .build(), NULL);

        for (final uk.gov.justice.json.schemas.domains.sjp.queries.Offence offence : caseDetails.getDefendant().getOffences()) {
            final String offenceCode = offence.getOffenceCode();
            final ZonedDateTime dateAndTimeOfSession = ZonedDateTimes.fromString(sjpSessionPayload.getString("startedAt", null));
            final String offenceDate = dateAndTimeOfSession != null ? dateAndTimeOfSession.toLocalDate().toString() : null;
            final Optional<JsonObject> offencesFromReferenceData = referenceDataOffencesService.getOffenceReferenceData(emptyEnvelope, offenceCode, offenceDate);

            if (offencesFromReferenceData.isPresent()) {
                final JsonObject offenceObject = offencesFromReferenceData.get();
                final Offence.Builder offenceBuilder = offence(
                        sjpSessionPayload,
                        offence,
                        offenceObject,
                        resultsAggregate);
                offences.add(offenceBuilder.build());
            }
        }
        return offences;
    }

    private Offence.Builder offence(final JsonObject sjpSessionPayload,
                                    final uk.gov.justice.json.schemas.domains.sjp.queries.Offence offence,
                                    final JsonObject offenceObject,
                                    final DecisionAggregate resultsAggregate) {

        final Plea plea = offence.getPleaDate() != null ?
                pleaConverter.getPlea(offence, fromString(sjpSessionPayload.getString("sessionId"))) : null;


        return getOffenceBuilderWithPopulation(offence, offenceObject, resultsAggregate, plea);
    }

    @VisibleForTesting
    Offence.Builder getOffenceBuilderWithPopulation(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence offence,
                                                            final JsonObject offenceObject,
                                                            final DecisionAggregate resultsAggregate,
                                                            final Plea plea) {
        final Verdict verdict =
                ofNullable(resultsAggregate.getConvictionInfo(offence.getId()))
                .map(e -> verdictConverter.getVerdict(e))
                .orElse(null);

        final LocalDate convictionDate =
                ofNullable(resultsAggregate.getConvictionInfo(offence.getId()))
                .map(ConvictionInfo::getConvictionDate)
                .orElse(null);

        final CourtCentre courtCentre = ofNullable(resultsAggregate.getConvictionInfo(offence.getId()))
                    .map(ConvictionInfo::getConvictingCourt)
                    .orElse(null);

        final List<JudicialResult> judicialResults = resultsAggregate.getResults(offence.getId());

        return Offence.offence()
                .withEndorsableFlag(offence.getEndorsable())
                .withId(offence.getId())//Mandatory
                .withOffenceDefinitionId(offence.getId())//Mandatory
                .withOffenceCode(offence.getOffenceCode())//Mandatory
                .withWording(offence.getWording())//Mandatory
                .withStartDate(offence.getStartDate())//Mandatory
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withEndDate(offence.getEndDate())
                .withChargeDate(offence.getChargeDate())
                .withOrderIndex(offence.getOffenceSequenceNumber())
                .withPlea(plea)
                .withIsDisposed(resultsAggregate.getFinalOffence(offence.getId()))
                .withVerdict(verdict)
                .withConvictionDate(ofNullable(convictionDate).map(e -> convictionDate.toString()).orElse(null))
                .withConvictingCourt(ofNullable(courtCentre).orElse(null))
                .withOffenceFacts(offenceFactsConverter.getOffenceFacts(offence))
                .withModeOfTrial(MODE_OF_TRIAL)
                .withJudicialResults(judicialResults.isEmpty() ?  null : judicialResults)
                .withOffenceTitle(ofNullable(offenceObject).map(e -> e.getString(TITLE, null)).orElse(null))
                .withOffenceLegislation(ofNullable(offenceObject).map(e -> e.getString("legislation", null)).orElse(null))
                .withOffenceTitleWelsh(ofNullable(offenceObject).map(e -> e.getString("welshoffencetitle", null)).orElse(null))
                .withOffenceLegislationWelsh(ofNullable(offenceObject).map(e -> e.getString("welshlegislation", null)).orElse(null))
                .withDvlaOffenceCode(getDVLAOffenceCode(offenceObject));
    }


    public String getDVLAOffenceCode(final JsonObject offenceReferenceData) {
        final Optional<JsonObject> document =
                ofNullable(offenceReferenceData.getJsonObject("details"))
                        .map(details -> details.getJsonObject("document"));

        if (document.isPresent()) {
            return ofNullable(document.get().getJsonObject("codes"))
                    .map(codes -> codes.getJsonObject("dvlacode"))
                    .map(dvlaCode -> dvlaCode.getString("code")).orElse(null);
        }
        return null;
    }


}
