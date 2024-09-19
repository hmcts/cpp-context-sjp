package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.command.AddCaseAssignmentRestriction;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;

import javax.json.JsonValue;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAssignmentRestrictionControllerTest {

    private static final String PROSECUTING_AUTHORITY = "TVL";
    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @InjectMocks
    private CaseAssignmentRestrictionController caseAssignmentRestrictionController;

    @Test
    public void shouldHandleAddCaseAssignmentRestrictionCommands() {
        assertThat(CaseAssignmentRestrictionController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("addCaseAssignmentRestriction").thatHandles("sjp.command.controller.add-case-assignment-restriction")));
    }

    @Test
    public void shouldSendNewAddCaseAssignmentRestrictionCommand() {
        final Envelope<AddCaseAssignmentRestriction> addCaseCaseAssignmentRestrictionCommand = createAddCaseCaseAssignmentRestrictionCommand();

        caseAssignmentRestrictionController.addCaseAssignmentRestriction(addCaseCaseAssignmentRestrictionCommand);

        final Matcher<? super ReadContext> newPayloadMatcher = allOf(
                withJsonPath("$.prosecutingAuthority", equalTo(PROSECUTING_AUTHORITY)),
                withJsonPath("$.includeOnly", hasSize(0)),
                withJsonPath("$.exclude", hasSize(1)),
                withJsonPath("$.exclude[0]", equalTo("1234")));

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> command = envelopeCaptor.getValue();
        assertThat(command.metadata(), JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom(addCaseCaseAssignmentRestrictionCommand).withName("sjp.command.add-case-assignment-restriction"));
        assertThat(command.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(newPayloadMatcher));
    }

    private static Envelope<AddCaseAssignmentRestriction> createAddCaseCaseAssignmentRestrictionCommand() {
        final AddCaseAssignmentRestriction addCaseAssignmentRestriction = new AddCaseAssignmentRestriction(singletonList("1234"), emptyList(), PROSECUTING_AUTHORITY);
        return Envelope.envelopeFrom(
                metadataWithRandomUUID("sjp.command.controller.add-case-assignment-restriction"),
                addCaseAssignmentRestriction);
    }
}
