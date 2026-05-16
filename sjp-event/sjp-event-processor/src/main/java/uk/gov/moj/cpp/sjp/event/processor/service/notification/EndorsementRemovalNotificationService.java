package uk.gov.moj.cpp.sjp.event.processor.service.notification;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper.jsonObjectAsByteArray;

import java.io.ByteArrayInputStream;

import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
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
import uk.gov.moj.cpp.sjp.filestore.azure.FileStorer;
import uk.gov.moj.cpp.sjp.filestore.azure.SasUriGenerator;
import uk.gov.moj.cpp.sjp.filestore.azure.StoragePath;

import java.net.URI;
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
    private SasUriGenerator sasUriGenerator;
    @Inject
    private SystemDocGenerator systemDocGenerator;

    public boolean hasEndorsementsToBeRemoved(final CaseDetailsDecorator caseDetails) {
        return caseDetails.getCurrentApplicationDecision()
                .map(this::hasEndorsementsToBeRemoved)
                .orElse(false);
    }

    public void generateNotification(final CaseDetailsDecorator caseDetails, final JsonEnvelope envelope) {
        final UUID correlationId = caseDetails.getCurrentApplicationDecision()
                .orElseThrow(IllegalStateException::new)
                .getId();

        final EndorsementRemovalNotificationTemplateData templatePayload = createTemplatePayload(caseDetails, envelope);

        final UUID fileId = storeEmailAttachmentTemplatePayload(correlationId, templatePayload);
        final URI payloadSourceUri = sasUriGenerator.generateReadUri(StoragePath.published("sdg-payloads"), fileId);
        requestEmailAttachmentGeneration(correlationId.toString(), fileId, payloadSourceUri, envelope);
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
                                                     final EndorsementRemovalNotificationTemplateData templateData) {
        final byte[] payloadBytes = jsonObjectAsByteArray(objectToJsonObjectConverter.convert(templateData));
        return fileStorer.store(StoragePath.published("sdg-payloads"), correlationId, fileName(correlationId), new ByteArrayInputStream(payloadBytes));
    }

    private void requestEmailAttachmentGeneration(final String sourceCorrelationId, final UUID fileId, final URI payloadSourceUri, final JsonEnvelope envelope) {
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT,
                ConversionFormat.PDF,
                sourceCorrelationId,
                fileId,
                payloadSourceUri
        );

        systemDocGenerator.generateDocument(request, envelope);
    }

    private String fileName(final UUID correlationId) {
        return String.format("notification-to-dvla-to-remove-endorsement-%s.pdf", correlationId);
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
