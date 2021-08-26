package uk.gov.moj.cpp.sjp.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

@SuppressWarnings({"squid:S1188"})
@ServiceComponent(EVENT_PROCESSOR)
public class CourtApplicationCreatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationCreatedProcessor.class);


    @Inject
    private Sender sender;


    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    private static String APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_CODE = "MC80528";
    private static String APPLICATION_TO_REOPEN_CASE_CODE = "MC80524";

    @Handles("public.progression.court-application-created")
    public void courtApplicationCreated(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final JsonObject courtApplicationJsonObject = payload.getJsonObject("courtApplication");


        final CourtApplication courtApplication = jsonObjectConverter.convert(courtApplicationJsonObject, CourtApplication.class);
        courtApplication.getCourtApplicationCases().stream(). filter(CourtApplicationCase::getIsSJP).forEach(courtApplicationCase -> {

            final String sjpCaseId = courtApplicationCase.getProsecutionCaseId().toString();
            String applicationStatus = null;
            final String applicationId = courtApplication.getId().toString();
            if(isNotBlank(courtApplication.getType().getCode()) && courtApplication.getType().getCode().equalsIgnoreCase(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_CODE)) {
                applicationStatus = ApplicationStatus.STATUTORY_DECLARATION_PENDING.name();
            }
            if(isNotBlank(courtApplication.getType().getCode()) && courtApplication.getType().getCode().equalsIgnoreCase(APPLICATION_TO_REOPEN_CASE_CODE)) {
                applicationStatus = ApplicationStatus.REOPENING_PENDING.name();
            }
            if((null != courtApplication.getType().getAppealFlag() && courtApplication.getType().getAppealFlag())
                    && (null != courtApplication.getType().getLinkType()) && courtApplication.getType().getLinkType().equals(LinkType.SJP)) {
                applicationStatus = ApplicationStatus.APPEAL_PENDING.name();
            }
            if(isNotBlank(applicationStatus)){
                final JsonObject applicationStatusPayload = Json.createObjectBuilder()
                        .add("caseId", sjpCaseId)
                        .add("applicationId", applicationId)
                        .add("applicationStatus", applicationStatus)
                        .build();
                final JsonEnvelope envelopeToSend = envelopeFrom(
                        metadataFrom(jsonEnvelope.metadata()).withName("sjp.command.update-cc-case-application-status"),
                        applicationStatusPayload);
                LOGGER.info("Command Raised for Criminal Court Application Created  is  sjp.command.update-cc-case-application-status  with CaseId   {} and applicationId  {} and applicationStatus  {} for sjp case", sjpCaseId, applicationId, applicationStatus);
                sender.send(envelopeToSend);

            }

        });

   }
}
