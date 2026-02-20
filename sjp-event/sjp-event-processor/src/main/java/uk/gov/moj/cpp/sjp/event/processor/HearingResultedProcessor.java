package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResultStatusResolver;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedProcessor.class);

    public static final String PUBLIC_EVENTS_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles(PUBLIC_EVENTS_HEARING_RESULTED)
    public void hearingResultReceived(final JsonEnvelope jsonEnvelope) {

        final HearingResulted publicHearingResulted = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingResulted.class);
        final Hearing hearing = publicHearingResulted.getHearing();
        if (TRUE.equals(hearing.getIsSJPHearing())) {
            LOGGER.info("public.events.hearing.hearing-resulted originating from SJP.  Hence ignoring");
            return;
        }

        if (hearing.getProsecutionCases() != null && hearing.getProsecutionCases().stream().anyMatch(e-> Objects.nonNull(e.getMigrationSourceSystem()))){
            LOGGER.info("public.events.hearing.hearing-resulted has migrationsource system.  Hence ignoring");
            return ;
        }

        LOGGER.info("public.events.hearing.hearing-resulted processing start");
        if (isEmpty(hearing.getCourtApplications())) {
            LOGGER.info("courtApplications is empty. no processing required");
            return;
        }

        hearing.getCourtApplications().stream().forEach(courtApplication -> {
            final String applicationType = courtApplication.getType().getType();
            final String applicationId = courtApplication.getId().toString();
            if(courtApplication.getCourtApplicationCases() != null ) {
                courtApplication
                        .getCourtApplicationCases()
                        .stream()
                        .filter(CourtApplicationCase::getIsSJP)
                        .forEach(courtApplicationCase -> {
                            final String sjpCaseId = courtApplicationCase.getProsecutionCaseId().toString();
                            final String applicationStatus = getApplicationStatus(courtApplication, applicationType);
                            sendMessage(jsonEnvelope, applicationId, sjpCaseId, applicationStatus);
                        });
            }
        });

    }

    private void sendMessage(final JsonEnvelope jsonEnvelope, final String applicationId, final String sjpCaseId, final String applicationStatus) {
        final JsonObject applicationStatusPayload = Json.createObjectBuilder()
                .add("caseId", sjpCaseId)
                .add("applicationId", applicationId)
                .add("applicationStatus", applicationStatus)
                .build();
        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(jsonEnvelope.metadata()).withName("sjp.command.update-cc-case-application-status"),
                applicationStatusPayload);
        sender.send(envelopeToSend);
    }

    private String getApplicationStatus(final CourtApplication courtApplication, final String applicationType) {

        final Optional<String> applicationStatus =
                courtApplication
                        .getJudicialResults()
                        .stream()
                        .map(judicialResult -> ApplicationResultStatusResolver.getApplicationStatus(applicationType, judicialResult.getJudicialResultTypeId()))
                        .filter(Objects::nonNull)
                        .findFirst();

        return applicationStatus.orElse(ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN.toString());
    }

}
