package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.REOPENING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper.jsonObjectAsByteArray;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationRequired;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.ConversionFormat;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.DocumentGenerationRequest;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.SystemDocGenerator;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S3655"}) // Suppress Optional.get without .isPresent().
public class EnforcementEmailAttachmentService {

    private static final String STAT_DECS_EMAIL_SUBJECT = "Subject: APPLICATION FOR A STATUTORY DECLARATION RECEIVED (COMMISSIONER OF OATHS)";
    private static final String REOPENING_EMAIL_SUBJECT = "Subject: APPLICATION TO REOPEN RECEIVED";

    private static final String STAT_DECS_EMAIL_TITLE = "APPLICATION FOR A STATUTORY DECLARATION RECEIVED (COMMISSIONER OF OATHS)";
    private static final String REOPENING_EMAIL_TITLE = "APPLICATION TO REOPEN RECEIVED";


    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;
    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private SjpService sjpService;

    @Inject
    private FileStorer fileStorer;
    @Inject
    private SystemDocGenerator systemDocGenerator;

    @Inject
    private EnforcementAreaEmailHelper enforcementAreaEmailHelper;


    public void generateNotification(final EnforcementPendingApplicationNotificationRequired enforcementPendingApplicationNotificationInitiated,
                                     final JsonEnvelope envelope) throws FileServiceException {
        final EnforcementPendingApplicationNotificationTemplateData templatePayload = createTemplatePayload(enforcementPendingApplicationNotificationInitiated, envelope);

        final UUID applicationId = enforcementPendingApplicationNotificationInitiated.getApplicationId();
        final UUID fileId = storeEmailAttachmentTemplatePayload(applicationId, templatePayload);
        requestEmailAttachmentGeneration(applicationId.toString(), fileId, envelope);
    }

    public String getEmailSubject(final ApplicationType applicationType) {
        if (null == applicationType) {
            throw new IllegalStateException(format("Invalid Application Type, unable to derive email subject for application type: %s", applicationType));
        }
        if (applicationType.equals(STAT_DEC)) {
            return STAT_DECS_EMAIL_SUBJECT;
        }
        if (applicationType.equals(REOPENING)) {
            return REOPENING_EMAIL_SUBJECT;
        }
        throw new IllegalStateException(format("Invalid Application Type, unable to derive email subject for application type: %s", applicationType));
    }


    public String getEmailTitle(final ApplicationType applicationType) {
        if (null == applicationType) {
            throw new IllegalStateException(format("Invalid Application Type, unable to derive email subject for application type: %s", applicationType));
        }
        if (applicationType.equals(STAT_DEC)) {
            return STAT_DECS_EMAIL_TITLE;
        }
        if (applicationType.equals(REOPENING)) {
            return REOPENING_EMAIL_TITLE;
        }
        throw new IllegalStateException(format("Invalid Application Type, unable to derive email subject for application type: %s", applicationType));
    }

    private EnforcementPendingApplicationNotificationTemplateData createTemplatePayload(final EnforcementPendingApplicationNotificationRequired initiated, JsonEnvelope envelope) {
        final CaseDetails caseDetails = sjpService.getCaseDetailsByApplicationId(initiated.getApplicationId(), envelope);
        String courtCentreName = "";
        if(isNotEmpty(caseDetails.getCaseDecisions())){
            final Session session = caseDetails.getCaseDecisions().get(0).getSession();
            if (nonNull(session)) {
                courtCentreName = session.getCourtHouseName();
            }
        }
        final String title = getEmailTitle(caseDetails.getCaseApplication().getApplicationType());
        return new EnforcementPendingApplicationNotificationTemplateDataBuilder()
                .withCaseReference(initiated.getUrn())
                .withDefendantName(initiated.getDefendantName())
                .withGobAccountNumber(initiated.getGobAccountNumber())
                .withDateApplicationIsListed(initiated.getDateApplicationIsListed())
                .withDivisionCode(initiated.getDivisionCode())
                .withTitle(title)
                .withDefendantAddress(initiated.getDefendantAddress())
                .withDefendantEmail(initiated.getDefendantEmail())
                .withDefendantContactNumber(initiated.getDefendantContactNumber())
                .withDefendantDateOfBirth(initiated.getDefendantDateOfBirth())
                .withOriginalDateOfSentence(initiated.getOriginalDateOfSentence())
                .withCourtCentreName(courtCentreName)
                .build();
    }

    private UUID storeEmailAttachmentTemplatePayload(final UUID applicationId,
                                                     final EnforcementPendingApplicationNotificationTemplateData templateData) throws FileServiceException {
        final JsonObject metadata = metadata(fileName(applicationId));
        return fileStorer.store(metadata, toInputStream(templateData));
    }

    private void requestEmailAttachmentGeneration(final String applicationId, final UUID fileId, final JsonEnvelope envelope) {
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                TemplateIdentifier.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION,
                ConversionFormat.PDF,
                applicationId,
                fileId
        );

        systemDocGenerator.generateDocument(request, envelope);
    }

    private JsonObject metadata(final String fileName) {
        return createObjectBuilder()
                .add("fileName", fileName)
                .add("conversionFormat", "pdf")
                .add("templateName", TemplateIdentifier.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.getValue())
                .build();
    }

    private String fileName(final UUID applicationId) {
        return format("enforcement-pending-application-%s.pdf", applicationId);
    }

    private ByteArrayInputStream toInputStream(final EnforcementPendingApplicationNotificationTemplateData templateData) {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(objectToJsonObjectConverter.convert(templateData));
        return new ByteArrayInputStream(jsonPayloadInBytes);
    }
}