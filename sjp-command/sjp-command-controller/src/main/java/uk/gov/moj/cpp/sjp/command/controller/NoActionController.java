package uk.gov.moj.cpp.sjp.command.controller;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.lang.annotation.Repeatable;

import javax.inject.Inject;

/**
 * Controller what directly forwards the payload from the API to the Handler
 * TODO: refactor once {@link Handles} becomes {@link Repeatable}
 */
@ServiceComponent(COMMAND_CONTROLLER)
public class NoActionController {

    @Inject
    private Sender sender;

    @Handles("sjp.command.action-court-referral")
    public void actionCourtReferral(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.add-case-document")
    public void addCaseDocument(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.cancel-request-withdrawal-all-offences")
    public void cancelRequestWithdrawalAllOffences(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.mark-case-reopened-in-libra")
    public void markCaseReopenedInLibra(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-case-reopened-in-libra")
    public void updateCaseReopenedInLibra(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.undo-case-reopened-in-libra")
    public void undoCaseReopenedInLibra(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.create-sjp-case")
    public void createSjpCase(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.add-dates-to-avoid")
    public void addDatesToAvoid(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-employer")
    public void updateEmployer(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.delete-employer")
    public void deleteEmployer(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.plead-online")
    public void pleadOnline(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.request-withdrawal-all-offences")
    public void requestWithdrawalAllOffences(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.start-session")
    public void startSession(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.end-session")
    public void endSession(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.migrate-session")
    public void migrateSession(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.assign-case")
    public void assignCase(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.unassign-case")
    public void unassignCase(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-defendant-details")
    public void updateDefendantDetails(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-defendant-national-insurance-number")
    public void updateDefendantNationalInsuranceNumber(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-financial-means")
    public void updateFinancialMeans(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-hearing-requirements")
    public void updateHearingRequirements(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-plea")
    public void updatePlea(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.cancel-plea")
    public void cancelPlea(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.upload-case-document")
    public void uploadCaseDocument(final JsonEnvelope envelope) {
        send(envelope);
    }

    private void send(final Envelope<?> envelope) {
        sender.send(envelope);
    }

}
