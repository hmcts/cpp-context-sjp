package uk.gov.moj.cpp.sjp.event.processor.service;


import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.APPLICATION_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CORRELATION_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.SESSION_ID;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDetailsDecorator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonValue;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class SjpService {

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;
    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    public CaseDetails getCaseDetails(final UUID caseId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.case"), payload);
        final Envelope<CaseDetails> caseDetailsEnvelope = requester.request(request, CaseDetails.class);

        return caseDetailsEnvelope.payload();
    }

    public CaseDetails getCaseDetailsByApplicationId(final UUID applicationId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(APPLICATION_ID, applicationId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.case-by-application-id"), payload);
        final Envelope<CaseDetails> caseDetailsEnvelope = requester.request(request, CaseDetails.class);

        return caseDetailsEnvelope.payload();
    }

    public CaseDetails getCaseDetailsByCorrelationId(final UUID correlationId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(CORRELATION_ID, correlationId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.case-by-correlation-id"), payload);
        final Envelope<CaseDetails> caseDetailsEnvelope = requester.request(request, CaseDetails.class);

        return caseDetailsEnvelope.payload();
    }

    public Optional<JsonObject> getConvictingCourtSessionDetails(final UUID offenceId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(OFFENCE_ID, offenceId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.convicting-court-session"), payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payload() != JsonValue.NULL
                ? ofNullable(response.payloadAsJsonObject())
                : empty();
    }

    public JsonObject getSessionDetails(final UUID sessionId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(SESSION_ID, sessionId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.session"), payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public JsonObject getLatestAocpSessionDetails(final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.latest-aocp-session"), payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(request, JsonObject.class);
        return response.payload();
    }

    public JsonEnvelope getSessionInformation(final UUID sessionId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(SESSION_ID, sessionId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.session"), payload);
        return requester.requestAsAdmin(request);
    }

    public DefendantsOnlinePlea getDefendantPleaDetails(final UUID caseId, final UUID defendantId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(DEFENDANT_ID, defendantId.toString())
                .build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.defendants-online-plea"), payload);
        final Envelope<DefendantsOnlinePlea> defendantsOnlinePleaEnvelope = requester.request(request, DefendantsOnlinePlea.class);

        return defendantsOnlinePleaEnvelope.payload();
    }

    public EmployerDetails getEmployerDetails(final UUID defendantId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(DEFENDANT_ID, defendantId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.employer"), payload);
        final Envelope<EmployerDetails> defendantsOnlinePleaEnvelope = requester.request(request, EmployerDetails.class);

        return defendantsOnlinePleaEnvelope.payload();
    }

    public List<JsonObject> getPendingCases(final JsonEnvelope envelope, final ExportType exportType) {
        return requester.request(
                        envelopeFrom(
                                metadataFrom(envelope.metadata())
                                        .withName("sjp.query.pending-cases").build(),
                                createObjectBuilder()
                                        .add("export", exportType.name().toLowerCase())
                                        .build()
                        ))
                .payloadAsJsonObject()
                .getJsonArray("pendingCases")
                .getValuesAs(JsonObject.class);
    }

    public List<JsonObject> getPendingDeltaCases(final JsonEnvelope envelope, final ExportType exportType) {

        final String eventName = "sjp.query.pending-delta-cases";

        final JsonObject jsonObjectBuilder = createObjectBuilder()
                .add("export", exportType.name().toLowerCase())
                .build();

        return requester.request(envelopeFrom(metadataFrom(envelope.metadata()).withName(eventName).build(), jsonObjectBuilder))
                .payloadAsJsonObject()
                .getJsonArray("pendingCases")
                .getValuesAs(JsonObject.class);
    }

    public void addAccountNumberToDefendant(final UUID caseId, final UUID correlationId,
                                            final String accountNumber, final JsonEnvelope sourceEnvelope) {

        final JsonObject commandPayload = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add("correlationId", correlationId.toString())
                .add("accountNumber", accountNumber)
                .build();

        sender.send(envelopeFrom(metadataFrom(sourceEnvelope.metadata())
                        .withName("sjp.command.add-financial-imposition-account-number"),
                commandPayload
        ));
    }

    public Optional<NotificationOfEndorsementStatus> getNotificationOfEndorsementStatus(final UUID applicationDecisionId, final JsonEnvelope envelope) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName("sjp.query.notification-of-endorsement-status"),
                createObjectBuilder()
                        .add("applicationDecisionId", applicationDecisionId.toString())
                        .build());

        final Envelope<NotificationOfEndorsementStatus> responseEnvelope = requester.request(request, NotificationOfEndorsementStatus.class);

        return Optional.ofNullable(responseEnvelope.payload());
    }

    public Optional<NotificationOfPartialAocpStatus> getNotificationOfPartialAocpStatus(final UUID caseId, final JsonEnvelope envelope) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName("sjp.query.notification-of-partial-aocp-status"),
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .build());

        final Envelope<NotificationOfPartialAocpStatus> responseEnvelope = requester.request(request, NotificationOfPartialAocpStatus.class);

        return Optional.ofNullable(responseEnvelope.payload());
    }

    public Optional<EnforcementPendingApplicationNotificationStatus> getEnforcementPendingApplicationNotificationStatus(final UUID applicationId, final JsonEnvelope envelope) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName("sjp.query.enforcement-pending-application-notification-status"),
                createObjectBuilder()
                        .add("applicationId", applicationId.toString())
                        .build());

        final Envelope<EnforcementPendingApplicationNotificationStatus> responseEnvelope = requester.request(request, EnforcementPendingApplicationNotificationStatus.class);

        return Optional.ofNullable(responseEnvelope.payload());
    }

    public Optional<AocpAcceptedEmailNotificationStatus> getAocpAcceptedEmailNotificationStatus(final UUID caseId, final JsonEnvelope envelope) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName("sjp.query.aocp-accepted-email-notification-status"),
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .build());

        final Envelope<AocpAcceptedEmailNotificationStatus> responseEnvelope = requester.request(request, AocpAcceptedEmailNotificationStatus.class);

        return Optional.ofNullable(responseEnvelope.payload());
    }

    public Optional<CaseDetailsDecorator> getCaseDetailsByApplicationDecisionId(final UUID applicationDecisionId,
                                                                                final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("applicationDecisionId", applicationDecisionId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.case-by-application-decision-id"), payload);
        final Envelope<CaseDetails> caseDetailsEnvelope = requester.request(request, CaseDetails.class);

        return Optional.ofNullable(caseDetailsEnvelope.payload()).map(CaseDetailsDecorator::new);
    }

}
