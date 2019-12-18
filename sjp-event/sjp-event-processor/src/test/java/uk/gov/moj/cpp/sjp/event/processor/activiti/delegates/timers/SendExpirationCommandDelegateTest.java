package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.timers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SendExpirationCommandDelegateTest  {

    @Mock
    private Sender sender;

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    private SendExpirationCommandDelegate delegate;

    @Test
    public void shouldFailValidationIfTheCommandNameIsNotValid() {
        final String commandName = "sjp.command.expire-timer";
        final UUID caseId = UUID.randomUUID();
        final Metadata metadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class))
                .thenReturn(metadataToString(metadata));
        when(delegateExecution.getVariable("commandToSend", String.class)).thenReturn(commandName);
        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());

        delegate.execute(delegateExecution);

        verify(sender).sendAsAdmin(argThat(
                jsonEnvelope(
                        metadata().withName(commandName),
                        payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString())))
                )));

    }

}