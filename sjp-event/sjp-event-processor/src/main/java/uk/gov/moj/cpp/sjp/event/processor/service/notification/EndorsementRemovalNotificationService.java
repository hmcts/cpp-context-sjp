package uk.gov.moj.cpp.sjp.event.processor.service.notification;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper.jsonObjectAsByteArray;

import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.models.ApplicationDecisionDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDecisionDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDetailsDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.models.OffenceDecisionDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.ConversionFormat;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.DocumentGenerationRequest;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.SystemDocGenerator;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S3655"}) // Suppress Optional.get without .isPresent().
public class EndorsementRemovalNotificationService {

    private static final String DVLA_CODE_TT99 = "TT99";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;
    @Inject
    private ReferenceDataService referenceDataService;
    @Inject
    private FileStorer fileStorer;
    @Inject
    private SystemDocGenerator systemDocGenerator;

    public boolean hasEndorsementsToBeRemoved(final CaseDetailsDecorator caseDetails) {
        return caseDetails.getCurrentApplicationDecision()
                .map(this::hasEndorsementsToBeRemoved)
                .orElse(false);
    }

    public void generateNotification(final CaseDetailsDecorator caseDetails, final JsonEnvelope envelope) throws FileServiceException {
        final UUID correlationId = caseDetails.getCurrentApplicationDecision()
                .orElseThrow(IllegalStateException::new)
                .getId();

        final EndorsementRemovalNotificationTemplateData templatePayload = createTemplatePayload(caseDetails, envelope);

        final UUID fileId = storeEmailAttachmentTemplatePayload(correlationId, templatePayload);
        requestEmailAttachmentGeneration(correlationId.toString(), fileId, envelope);
    }

    public String buildEmailSubject(final ApplicationDecisionDecorator applicationDecision, final JsonEnvelope envelope) {
        final CaseDetailsDecorator caseDetails = applicationDecision.getCaseDetails();
        final String defendantDateOfBirth = caseDetails.getDefendantDateOfBirth()
                .map(EndorsementRemovalNotificationTemplateDataBuilder::formatDate)
                .orElse(null);

        return new EndorsementRemovalNotificationEmailSubject(
                getLjaName(applicationDecision.getLocalJusticeAreaNationalCourtCode(), envelope),
                caseDetails.getDefendantFirstName(),
                caseDetails.getDefendantLastName(),
                defendantDateOfBirth,
                caseDetails.getUrn()
        ).toString();
    }

    private EndorsementRemovalNotificationTemplateData createTemplatePayload(final CaseDetailsDecorator caseDetails,
                                                                             final JsonEnvelope envelope) {

        final ApplicationDecisionDecorator applicationDecision = caseDetails.getCurrentApplicationDecision().get();

        final String ljaCode = applicationDecision.getLocalJusticeAreaNationalCourtCode();
        final String ljaName = getLjaName(ljaCode, envelope);

        final EndorsementRemovalNotificationTemplateDataBuilder templateData = new EndorsementRemovalNotificationTemplateDataBuilder()
                .withDateOfOrder(applicationDecision.getSavedAt().toLocalDate())
                .withLjaCode(ljaCode)
                .withLjaName(ljaName)
                .withCaseUrn(caseDetails.getUrn())
                .withDefendant(caseDetails.getDefendant())
                .withReasonForIssue(applicationDecision.getApplicationType());

        templateData.withDrivingEndorsementsToBeRemoved(createDrivingEndorsementsToBeRemoved(caseDetails, envelope));

        return templateData.build();
    }

    private UUID storeEmailAttachmentTemplatePayload(final UUID correlationId,
                                                     final EndorsementRemovalNotificationTemplateData templateData) throws FileServiceException {
        final JsonObject metadata = metadata(fileName(correlationId));
        return fileStorer.store(metadata, toInputStream(templateData));
    }

    private void requestEmailAttachmentGeneration(final String sourceCorrelationId, final UUID fileId, final JsonEnvelope envelope) {
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT,
                ConversionFormat.PDF,
                sourceCorrelationId,
                fileId
        );

        systemDocGenerator.generateDocument(request, envelope);
    }

    private JsonObject metadata(final String fileName) {
        return createObjectBuilder()
                .add("fileName", fileName)
                .add("conversionFormat", "pdf")
                .add("templateName", TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue())
                .build();
    }

    private String fileName(final UUID correlationId) {
        return String.format("notification-to-dvla-to-remove-endorsement-%s.pdf", correlationId);
    }

    private ByteArrayInputStream toInputStream(final EndorsementRemovalNotificationTemplateData templateData) {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(objectToJsonObjectConverter.convert(templateData));
        return new ByteArrayInputStream(jsonPayloadInBytes);
    }

    private boolean hasEndorsementsToBeRemoved(final ApplicationDecisionDecorator applicationDecision) {
        return applicationDecision.getPreviousFinalDecision().hasEndorsementsOrDisqualification();
    }

    private JsonObject getOffenceReferenceData(final JsonEnvelope envelope, final Offence offence) {
        return referenceDataOffencesService.getOffenceReferenceData(envelope, offence.getOffenceCode(), offence.getStartDate())
                .orElseThrow(IllegalArgumentException::new);
    }

    private String getLjaName(final String ljaCode, final JsonEnvelope envelope) {
        return referenceDataService.getLocalJusticeAreaByCode(envelope, ljaCode).getName();
    }

    private List<DrivingEndorsementToBeRemoved> createDrivingEndorsementsToBeRemoved(final CaseDetailsDecorator caseDetails,
                                                                                     final JsonEnvelope envelope) {
        final CaseDecisionDecorator previousFinalDecision = caseDetails.getCurrentApplicationDecision().get()
                .getPreviousFinalDecision();

        return previousFinalDecision.getOffenceDecisionsWithEndorsementOrDisqualification()
                .stream()
                .map(OffenceDecisionDecorator::new)
                .map(offenceDecision -> createDrivingEndorsementToBeRemoved(offenceDecision, caseDetails, envelope))
                .collect(Collectors.toList());
    }

    private DrivingEndorsementToBeRemoved createDrivingEndorsementToBeRemoved(final OffenceDecisionDecorator offenceDecision,
                                                                              final CaseDetailsDecorator caseDetails,
                                                                              final JsonEnvelope envelope) {

        final ApplicationDecisionDecorator applicationDecision = caseDetails.getCurrentApplicationDecision().get();
        final CaseDecisionDecorator previousFinalDecision = applicationDecision.getPreviousFinalDecision();
        final String ljaCode = applicationDecision.getLocalJusticeAreaNationalCourtCode();
        final Offence offence = caseDetails.getOffenceById(offenceDecision.getOffenceId());
        final JsonObject offenceReferenceData = getOffenceReferenceData(envelope, offence);
        final String originalConvictionDate = ofNullable(offenceDecision.getConvictionDate()).orElse(
                previousFinalDecision.getSavedAt().toLocalDate()).toString();
        final String dvlaCode = offenceDecision.hasPointsDisqualification() ? DVLA_CODE_TT99 : offenceReferenceData.getString("dvlaCode", null);

        return new DrivingEndorsementToBeRemoved(
                offenceReferenceData.getString("title"),
                ljaCode,
                originalConvictionDate,
                dvlaCode,
                offence.getStartDate()
        );
    }
}
