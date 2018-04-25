package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;

import java.time.LocalDate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseStartedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private CaseStartedDelegate caseStartedDelegate;

    @Test
    public void shouldEmitPublicEvent() {

        final LocalDate postingDate = LocalDate.now();

        when(delegateExecution.getVariable(POSTING_DATE_VARIABLE, String.class)).thenReturn(postingDate.format(ISO_DATE));

        caseStartedDelegate.execute(delegateExecution);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("public.sjp.sjp-case-created"),
                        payloadIsJson(allOf(
                                withJsonPath("$.id", equalTo(caseId.toString())),
                                withJsonPath("$.postingDate", equalTo(postingDate.format(ISO_DATE)))
                        ))
                )));
    }
}
