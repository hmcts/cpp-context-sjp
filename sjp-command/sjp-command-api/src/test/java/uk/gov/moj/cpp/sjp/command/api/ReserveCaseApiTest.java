package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.reserveCaseGroups;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.undoReserveCaseGroups;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReserveCaseApiTest extends BaseDroolsAccessControlTest {


    public static final String RESERVE_CASE = "sjp.reserve-case";
    public static final String SJP_UNDO_RESERVE_CASE = "sjp.undo-reserve-case";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private ReserveCaseApi reserveCaseApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Test
    public void shouldAllowAuthorisedUserToReserveCaseStatus() {
        final Action action = createActionFor(RESERVE_CASE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, reserveCaseGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToUndoReserveCaseStatus() {
        final Action action = createActionFor(SJP_UNDO_RESERVE_CASE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, undoReserveCaseGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldReserveCaseStatusCommand() {
        final JsonEnvelope commandEnvelope = envelope().
                with(metadataWithRandomUUID(RESERVE_CASE))
                .build();

        reserveCaseApi.reserveCaseStatus(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentCommandEnvelope = envelopeCaptor.getValue();
        assertThat(sentCommandEnvelope.metadata().name(), is("sjp.command.reserve-case"));
        assertThat(commandEnvelope.payloadAsJsonObject(), Matchers.equalTo(sentCommandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldUndoReserveCaseStatusCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(SJP_UNDO_RESERVE_CASE)).build();

        reserveCaseApi.undoReserveCaseStatus(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("sjp.command.undo-reserve-case"));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }
}
