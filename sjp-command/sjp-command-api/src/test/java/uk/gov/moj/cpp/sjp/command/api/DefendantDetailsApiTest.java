package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Collections.singletonMap;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getUpdateDefendantDetails;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildAddressWithPostcode;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantDetailsApiTest extends BaseDroolsAccessControlTest {

    private static final String UPDATE_DEFENDANT_COMMAND_NAME = "sjp.update-defendant-details";
    private static final String UPDATE_DEFENDANT_NEW_COMMAND_NAME = "sjp.command.update-defendant-details";
    private static final String UPDATE_NINO_COMMAND_NAME = "sjp.update-defendant-national-insurance-number";
    private static final String UPDATE_NINO_NEW_COMMAND_NAME = "sjp.command.update-defendant-national-insurance-number";
    private static final String ACKNOWLEDGE_DEFENDANT_UPDATES_COMMAND_NAME = "sjp.acknowledge-defendant-details-updates";
    private static final String ACKNOWLEDGE_DEFENDANT_UPDATES_NEW_COMMAND_NAME = "sjp.command.acknowledge-defendant-details-updates";
    private static final String ACCEPT_PENDING_DEFENDANT_CHANGES_COMMAND_NAME = "sjp.accept-pending-defendant-changes";
    private static final String ACCEPT_PENDING_DEFENDANT_CHANGES_NEW_COMMAND_NAME = "sjp.command.accept-pending-defendant-changes";
    private static final String REJECT_PENDING_DEFENDANT_CHANGES_COMMAND_NAME = "sjp.reject-pending-defendant-changes";
    private static final String REJECT_PENDING_DEFENDANT_CHANGES_NEW_COMMAND_NAME = "sjp.command.reject-pending-defendant-changes";

    @Spy
    @SuppressWarnings("unused")
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private DefendantDetailsApi defendantDetailsApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public DefendantDetailsApiTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToUploadCaseDocument() {
        final Action action = createActionFor(UPDATE_DEFENDANT_COMMAND_NAME);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateDefendantDetails()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldHandleCommands() {
        assertThat(DefendantDetailsApi.class, isHandlerClass(COMMAND_API)
                .with(method("updateDefendantDetails").thatHandles(UPDATE_DEFENDANT_COMMAND_NAME))
                .with(method("updateDefendantNationalInsuranceNumber").thatHandles(UPDATE_NINO_COMMAND_NAME))
                .with(method("acknowledgeDefendantDetailsUpdates").thatHandles(ACKNOWLEDGE_DEFENDANT_UPDATES_COMMAND_NAME))
                .with(method("acceptPendingDefendantChanges").thatHandles(ACCEPT_PENDING_DEFENDANT_CHANGES_COMMAND_NAME)));
    }

    @Test
    public void shouldRenameUpdateDefendantDetailsCommand() {
        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(UPDATE_DEFENDANT_COMMAND_NAME), createObjectBuilder());

        defendantDetailsApi.updateDefendantDetails(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(UPDATE_DEFENDANT_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    @Test
    public void shouldRenameUpdateDefendantDetailsCommandWithAddress() {
        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(UPDATE_DEFENDANT_COMMAND_NAME), createObjectBuilder()
                .add("address", buildAddressWithPostcode("se11pj")));

        final JsonObject expectedPayload = createObjectBuilder()
                .add("address", buildAddressWithPostcode("SE1 1PJ"))
                .build();

        defendantDetailsApi.updateDefendantDetails(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(UPDATE_DEFENDANT_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(expectedPayload));
    }

    @Test
    public void shouldRenameUpdateNinoCommand() {
        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(UPDATE_NINO_COMMAND_NAME), createObjectBuilder());

        defendantDetailsApi.updateDefendantNationalInsuranceNumber(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(UPDATE_NINO_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    @Test
    public void shouldExtractRouteParamsWhenCreatingAcknowledgeDefendantUpdatesCommand() {
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        final JsonEnvelope command = envelopeFrom(
                metadataWithRandomUUID(ACKNOWLEDGE_DEFENDANT_UPDATES_COMMAND_NAME),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("defendantId", defendantId.toString()));

        defendantDetailsApi.acknowledgeDefendantDetailsUpdates(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(ACKNOWLEDGE_DEFENDANT_UPDATES_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    @Test
    public void shouldReplaceBlankEmailToNull() {
        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(UPDATE_DEFENDANT_COMMAND_NAME), createObjectBuilder()
                .add("email", "  "));

        final JsonObject expectedPayload = createObjectBuilder().build();

        defendantDetailsApi.updateDefendantDetails(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(UPDATE_DEFENDANT_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(expectedPayload));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesCommand() {
        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(ACCEPT_PENDING_DEFENDANT_CHANGES_COMMAND_NAME), createObjectBuilder()
                .add("address", buildAddressWithPostcode("cr05qt")));

        final JsonObject expectedPayload = createObjectBuilder()
                .add("address", buildAddressWithPostcode("CR0 5QT"))
                .build();

        defendantDetailsApi.acceptPendingDefendantChanges(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(ACCEPT_PENDING_DEFENDANT_CHANGES_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(expectedPayload));
    }

    @Test
    public void shouldRejectPendingDefendantChangesCommand() {
        final String caseId = "4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d";
        final String defendantId = "5ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1e";

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(REJECT_PENDING_DEFENDANT_CHANGES_COMMAND_NAME), createObjectBuilder()
                .add("caseId", caseId)
                .add("defendantId", defendantId));

        final JsonObject expectedPayload = createObjectBuilder()
                .add("caseId", caseId)
                .add("defendantId", defendantId)
                .build();

        defendantDetailsApi.rejectPendingDefendantChanges(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(REJECT_PENDING_DEFENDANT_CHANGES_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(expectedPayload));
    }

}
