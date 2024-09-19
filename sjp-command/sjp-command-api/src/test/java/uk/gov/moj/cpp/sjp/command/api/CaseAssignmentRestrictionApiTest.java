package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.command.AddCaseAssignmentRestriction;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAssignmentRestrictionApiTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @SuppressWarnings("unused")
    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @Spy
    @SuppressWarnings("unused")
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);


    @InjectMocks
    private CaseAssignmentRestrictionApi caseAssignmentRestrictionApi;

    @Test
    public void shouldHandleCaseAssignmentRestrictionCommands() {
        assertThat(CaseAssignmentRestrictionApi.class, isHandlerClass(COMMAND_API)
                .with(method("addCaseAssignmentRestriction").thatHandles("sjp.add-case-assignment-restriction")));
    }

    @Test
    public void shouldRequestAddCaseAssignmentRestrictionController() {
        final List<String> exclude = singletonList("1234");
        final List<String> includeOnly = emptyList();
        final AddCaseAssignmentRestriction envelopePayload = new AddCaseAssignmentRestriction(exclude, includeOnly, "TVL");
        final Envelope<AddCaseAssignmentRestriction> command = envelopeFrom(
                metadataWithRandomUUID("sjp.command.add-case-assignment-restriction"),
                envelopePayload);

        caseAssignmentRestrictionApi.addCaseAssignmentRestriction(command);
        verify(sender).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo("sjp.command.controller.add-case-assignment-restriction"));
        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(envelopePayload)));
    }
}
