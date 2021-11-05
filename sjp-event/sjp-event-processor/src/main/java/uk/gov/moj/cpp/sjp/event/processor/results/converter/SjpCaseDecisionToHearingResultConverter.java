package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.core.courts.HearingLanguage.ENGLISH;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted.publicHearingResulted;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionSavedToJudicialResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SjpCaseDecisionToHearingResultConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpCaseDecisionToHearingResultConverter.class);

    @Inject
    private CourtCentreConverter courtCenterConverter;

    @Inject
    private HearingDaysConverter hearingDaysConverter;

    @Inject
    private SjpService sjpService;

    @Inject
    private ProsecutionCasesConverter prosecutionCasesConverter;

    @Inject
    private DecisionSavedToJudicialResultsConverter referencedDecisionSavedOffenceConverter;

    // return the hearing object structure
    @SuppressWarnings("squid:S2629")
    public PublicHearingResulted convertCaseDecision(final Envelope<DecisionSaved> decisionSavedEventEnvelope) {

        // convert the results
        final DecisionSaved decisionSaved = decisionSavedEventEnvelope.payload();
        final UUID caseId = decisionSaved.getCaseId();
        final Metadata sourceMetadata = decisionSavedEventEnvelope.metadata();
        final JsonEnvelope sjpSessionEnvelope = sjpService.getSessionInformation(fromString(decisionSaved.getSessionId().toString()), envelopeFrom(metadataFrom(sourceMetadata), NULL));
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseId, envelopeFrom(metadataFrom(decisionSavedEventEnvelope.metadata()), NULL));
        final UUID defendantId = caseDetails.getDefendant().getId();
        final String driverNumber = caseDetails.getDefendant().getPersonalDetails().getDriverNumber();
        final String prosecutingAuthority = caseDetails.getProsecutingAuthority();

        final DecisionAggregate resultsAggregate =
                referencedDecisionSavedOffenceConverter.convertOffenceDecisions(
                        decisionSaved,
                        sjpSessionEnvelope,
                        defendantId,
                        decisionSaved.getSavedAt(),
                        driverNumber,
                        prosecutingAuthority,null);

        final List<DefendantJudicialResult> defendantJudicialResults = ofNullable(resultsAggregate
                .getResults(defendantId)).orElse(new ArrayList<>())
                .stream()
                .map(e -> DefendantJudicialResult
                        .defendantJudicialResult()
                        .withJudicialResult(e)
                        .withMasterDefendantId(defendantId)
                        .build())
                .collect(toList());

        final Hearing.Builder hearingBuilder = Hearing.hearing()
                .withId(decisionSaved.getDecisionId()) // mandatory
                .withJurisdictionType(MAGISTRATES) // mandatory
                .withCourtCentre(courtCenterConverter.convert(decisionSaved.getSessionId(), sourceMetadata)) // mandatory
                .withIsSJPHearing(true)
                .withHearingLanguage(ENGLISH)
                .withHearingDays(hearingDaysConverter.convert(sjpSessionEnvelope.payloadAsJsonObject()))
                .withHasSharedResults(false)
                .withDefendantJudicialResults(!defendantJudicialResults.isEmpty() ? defendantJudicialResults : null)
                .withProsecutionCases(prosecutionCasesConverter.convert(sjpSessionEnvelope.payloadAsJsonObject(), caseDetails, sourceMetadata, resultsAggregate))
                .withIsBoxHearing(false);

        final PublicHearingResulted publicHearingResulted = publicHearingResulted()
                .withHearing(hearingBuilder.build())
                .withSharedTime(decisionSavedEventEnvelope
                        .payload()
                        .getSavedAt())
                .build();

        LOGGER.info("[publicHearingResulted for Case]:{}", caseId);

        return publicHearingResulted;
    }

    // return the hearing object structure
    @SuppressWarnings("squid:S2629")
    public PublicHearingResulted convertCaseDecisionInCcForReferToCourt(final Envelope<CaseListedInCriminalCourtsV2>  caseListedInCcForReferToCourtEnvelope) {

        // convert the results
        final DecisionSaved decisionSaved = caseListedInCcForReferToCourtEnvelope.payload().getDecisionSaved();
        final UUID caseId = caseListedInCcForReferToCourtEnvelope.payload().getCaseId();
        final Metadata sourceMetadata = caseListedInCcForReferToCourtEnvelope.metadata();
        final JsonEnvelope sjpSessionEnvelope = sjpService.getSessionInformation(fromString(decisionSaved.getSessionId().toString()), envelopeFrom(metadataFrom(sourceMetadata), NULL));
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseId, envelopeFrom(metadataFrom(caseListedInCcForReferToCourtEnvelope.metadata()), NULL));
        final UUID defendantId = caseDetails.getDefendant().getId();
        final String driverNumber = caseDetails.getDefendant().getPersonalDetails().getDriverNumber();
        final String prosecutingAuthority = caseDetails.getProsecutingAuthority();

        final DecisionAggregate resultsAggregate =
                referencedDecisionSavedOffenceConverter.convertOffenceDecisions(
                        decisionSaved,
                        sjpSessionEnvelope,
                        defendantId,
                        decisionSaved.getSavedAt(),
                        driverNumber,
                        prosecutingAuthority,
                        caseListedInCcForReferToCourtEnvelope.payload());

        final List<DefendantJudicialResult> defendantJudicialResults = resultsAggregate
                .getResults(caseId)
                .stream()
                .map(e -> DefendantJudicialResult
                        .defendantJudicialResult()
                        .withJudicialResult(e)
                        .withMasterDefendantId(defendantId)
                        .build())
                .collect(toList());

        final Hearing.Builder hearingBuilder = Hearing.hearing()
                .withId(decisionSaved.getSessionId()) // mandatory
                .withJurisdictionType(MAGISTRATES) // mandatory
                .withCourtCentre(courtCenterConverter.convert(decisionSaved.getSessionId(), sourceMetadata)) // mandatory
                .withIsSJPHearing(true)
                .withHearingLanguage(ENGLISH)
                .withHearingDays(hearingDaysConverter.convert(sjpSessionEnvelope.payloadAsJsonObject()))
                .withHasSharedResults(false)
                .withDefendantJudicialResults(!defendantJudicialResults.isEmpty() ? defendantJudicialResults : null)
                .withProsecutionCases(prosecutionCasesConverter.convert(sjpSessionEnvelope.payloadAsJsonObject(), caseDetails, sourceMetadata, resultsAggregate))
                .withIsBoxHearing(false);

        final PublicHearingResulted publicHearingResulted = publicHearingResulted()
                .withHearing(hearingBuilder.build())
                .withSharedTime(caseListedInCcForReferToCourtEnvelope
                        .payload().getDecisionSaved()
                        .getSavedAt())
                .build();
        
        return publicHearingResulted;
    }


}
