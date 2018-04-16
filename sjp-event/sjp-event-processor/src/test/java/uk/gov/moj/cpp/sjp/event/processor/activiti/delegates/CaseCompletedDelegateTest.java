package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


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

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseCompletedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private CaseCompletedDelegate caseCompletedDelegate;

    @Test
    public void shouldSendCaseCompletedCommand() {

        caseCompletedDelegate.execute(delegateExecution);

        verify(sender).sendAsAdmin(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("sjp.command.complete-case"),
                        payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString())))
                )));
    }
}
