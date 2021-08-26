package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.HearingLanguage.ENGLISH;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;
import static uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted.publicHearingResulted;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper.toJsonString;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper.LABEL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper.SESSION_ID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper.STARTED_AT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper.populatePromptDefinitionAttributes;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper.populateResultDefinitionAttributes;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SjpApplicationDecisionToHearingResultConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpApplicationDecisionToHearingResultConverter.class);

    private static final String QUERY_COMMON_CASE_APPLICATION = "sjp.query.common-case-application";
    public static final String OUCODE = "oucode";

    @Inject
    private SjpService sjpService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private HearingDaysConverter hearingDaysConverter;

    @Inject
    private CourtCentreConverter courtCenterConverter;

    @Inject
    private ProsecutionCasesConverter prosecutionCasesConverter;

    @Inject
    private JCachedReferenceData jCachedReferenceData;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    // return the hearing object structure
    @SuppressWarnings("squid:S2629")
    public PublicHearingResulted convertApplicationDecision(final Envelope<ApplicationDecisionSaved> applicationDecisionSavedEnvelope) {

        final ApplicationDecisionSaved applicationDecisionSaved = applicationDecisionSavedEnvelope.payload();
        final ZonedDateTime savedOn = applicationDecisionSaved.getSavedAt();
        final Metadata sourceMetadata = applicationDecisionSavedEnvelope.metadata();
        final UUID caseId = applicationDecisionSaved.getCaseId();
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseId, envelopeFrom(metadataFrom(applicationDecisionSavedEnvelope.metadata()), NULL));
        final JsonEnvelope sjpSessionEnvelope = sjpService.getSessionInformation(fromString(applicationDecisionSaved.getSessionId().toString()), envelopeFrom(metadataFrom(sourceMetadata), NULL));

        // latest application decision
        if (caseDetails.getCaseApplication() != null) {
            LOGGER.info("BEFORE:publishing public.sjp.case-resulted for case {} for application", caseId);
            final PublicHearingResulted publicHearingResulted = handleApplicationDecision(sourceMetadata, sjpSessionEnvelope, caseDetails, savedOn);
            LOGGER.info("[publicHearingResulted for Application]:{}", toJsonString(publicHearingResulted));

            LOGGER.info("AFTER:publishing public.sjp.case-resulted for case {} for application", caseId);
            return publicHearingResulted;
        }

        return publicHearingResulted().build();

    }

    private PublicHearingResulted handleApplicationDecision(final Metadata sourceMetadata,
                                                            final JsonEnvelope sjpSessionEnvelope,
                                                            final CaseDetails caseDetails,
                                                            final ZonedDateTime savedOn) {

        final JsonEnvelope commonCaseApplication = getCommonCaseApplication(sourceMetadata, caseDetails.getId().toString());
        final CourtApplication courtApplicationFromQuery = jsonObjectToObjectConverter.convert(commonCaseApplication.payloadAsJsonObject(), CourtApplication.class);
        final JsonEnvelope emptyEnvelope = envelopeFrom(Envelope.metadataFrom(sjpSessionEnvelope.metadata()).
                withName("referencedata.query.prosecutors")
                .withId(UUID.randomUUID())
                .build(), NULL);
        final Optional<JsonObject> prosecutor = referenceDataService.getProsecutor(caseDetails.getProsecutingAuthority(), emptyEnvelope);

        final CourtApplication courtApplication =
                CourtApplication
                        .courtApplication()
                        .withValuesFrom(courtApplicationFromQuery)
                        .withApplicant(courtApplicationParty()
                                .withValuesFrom(courtApplicationFromQuery.getApplicant())
                                .withProsecutingAuthority(
                                        ofNullable(courtApplicationFromQuery.getApplicant().getProsecutingAuthority())
                                                .map(e -> prosecutingAuthority()
                                                        .withValuesFrom(e)
                                                        .withProsecutionAuthorityOUCode(prosecutor.map(p -> p.getString(OUCODE, null)).orElse(null))
                                                        .build())
                                                .orElse(null)
                                )
                                .build())
                        .build();

        final JsonObject sjpSessionPayload = sjpSessionEnvelope.payloadAsJsonObject();
        final ZonedDateTime zonedDateTime  = ZonedDateTime.parse(sjpSessionPayload.getString(STARTED_AT, null));
        final LocalDate sessionStartDate = LocalDate.of(zonedDateTime.getYear(), zonedDateTime.getMonth(), zonedDateTime.getDayOfMonth());
        final UUID sessionId = fromString(sjpSessionPayload.getString(SESSION_ID));

        final List<JudicialResult> judicialResultList = new ArrayList<>();
        courtApplication
                .getJudicialResults()
                .forEach(judicialResult -> {
                    final UUID resultId = judicialResult.getJudicialResultId();
                    final JsonObject resultDefinition = jCachedReferenceData.getResultDefinition(resultId, JsonEnvelope.envelopeFrom(sjpSessionEnvelope.metadata(), null), sessionStartDate);
                    final JudicialResult.Builder judicialResultBuilder = populateResultDefinitionAttributes(resultId, sessionId, resultDefinition);

                    final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
                    populateJudicialResultPrompts(judicialResult, resultDefinition, judicialResultPrompts);

                    judicialResultBuilder.withOrderedDate(savedOn.format(DATE_FORMAT));
                    judicialResultBuilder.withOrderedHearingId(sessionId);
                    judicialResultBuilder.withResultText(JudicialResultHelper.getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)));
                    judicialResultBuilder.withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts));

                    judicialResultList.add(judicialResultBuilder.build());
                });

        // replace the list
        courtApplication.getJudicialResults().clear();
        courtApplication.getJudicialResults().addAll(judicialResultList);

        final Hearing.Builder hearingBuilder = Hearing.hearing();
        hearingBuilder
                .withId(sessionId)//Mandatory
                .withJurisdictionType(MAGISTRATES)//Mandatory
                .withCourtCentre(courtCenterConverter.convert(sessionId, sourceMetadata))//Mandatory
                .withIsSJPHearing(true)
                .withHearingLanguage(ENGLISH)
                .withHearingDays(hearingDaysConverter.convert(sjpSessionPayload))
                .withCourtApplications(Collections.singletonList(courtApplication))
                .withHasSharedResults(false)
                .withHearingCaseNotes(null)
                .withIsBoxHearing(false);

        return publicHearingResulted()
                .withHearing(hearingBuilder.build())
                .withSharedTime(savedOn)
                .build();
    }

    private void populateJudicialResultPrompts(final JudicialResult judicialResult, final JsonObject resultDefinition, final List<JudicialResultPrompt> judicialResultPrompts) {
        judicialResult
                .getJudicialResultPrompts()
                .forEach(judicialResultPrompt -> {
                    final JudicialResultPrompt.Builder judicialResultPromptBuilder =
                            populatePromptDefinitionAttributes(Arrays.stream(JPrompt.values())
                                    .filter(e -> e.getId().equals(judicialResultPrompt.getJudicialResultPromptTypeId()))
                                    .findFirst().get(), resultDefinition);

                    judicialResultPromptBuilder.withValue(judicialResultPrompt.getValue());
                    ofNullable(judicialResultPrompt.getPromptReference())
                            .ifPresent(judicialResultPromptBuilder::withPromptReference);
                    judicialResultPrompts.add(judicialResultPromptBuilder.build());
                });
    }

    private JsonEnvelope getCommonCaseApplication(final Metadata sourceMetadata, final String caseId) {
        final JsonEnvelope caseResultsRequestEnvelope =
                envelopeFrom(metadataFrom(sourceMetadata)
                        .withName(QUERY_COMMON_CASE_APPLICATION), createObjectBuilder().add("caseId", caseId));
        return requester.request(caseResultsRequestEnvelope);
    }

    @SuppressWarnings("squid:S1151")
    protected List<JudicialResultPrompt> getCheckJudicialPromptsEmpty(final List<JudicialResultPrompt> resultPrompts) {
        return !resultPrompts.isEmpty() ? resultPrompts : null;
    }

}
