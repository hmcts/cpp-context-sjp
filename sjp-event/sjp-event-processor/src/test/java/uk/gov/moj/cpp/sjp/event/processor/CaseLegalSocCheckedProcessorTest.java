package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.service.UsersGroupsService;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseLegalSocCheckedProcessorTest {
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private CaseLegalSocCheckedProcessor caseLegalSocCheckedProcessor;

    @Mock
    protected Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @Mock
    private UsersGroupsService usersGroupsService;

    private UUID caseId;
    private UUID checkedBy;
    private String checkedAt;
    private JsonObject legalAdviserDetails;

    public static final String CHECKED_BY = "checkedBy";
    public static final String CHECKED_AT = "checkedAt";
    private static final String LEGAL_ADVISER_FIRST_NAME = "Erica";
    private static final String LEGAL_ADVISER_LAST_NAME = "Smith";

    @Test
    public void shouldHandleCaseNoteAddedEvent() {
        assertThat(CaseLegalSocCheckedProcessor.class, isHandlerClass(EVENT_PROCESSOR).with(
                method("handleCaseLegalSocChecked").thatHandles("sjp.events.marked-as-legal-soc-checked")));
    }

    @Test
    public void shouldRaisePublicEvent() {
        caseId = randomUUID();
        checkedBy = randomUUID();
        checkedAt = ZonedDateTimes.toString(ZonedDateTime.now());
        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope("sjp.events.marked-as-legal-soc-checked",
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(CHECKED_BY, checkedBy.toString())
                        .add(CHECKED_AT, checkedAt)
                        .build()
        );

        legalAdviserDetails = buildLegalAdviserDetails();
        when(usersGroupsService.getUserDetails(any(), any(JsonEnvelope.class))).thenReturn(legalAdviserDetails);

        caseLegalSocCheckedProcessor.handleCaseLegalSocChecked(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), equalTo(CaseLegalSocCheckedProcessor.CASE_LEGAL_SOC_CHECKED_PUBLIC_EVENT_NAME));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                                withJsonPath("$.caseId", CoreMatchers.equalTo(caseId.toString())),
                                withJsonPath("$.checkedBy", CoreMatchers.equalTo(checkedBy.toString())),
                                withJsonPath("$.checkedAt", CoreMatchers.equalTo(checkedAt))
                        )
                )
        );
    }

    @Test
    public void shouldRaiseAddNoteCommand() {
        caseId = randomUUID();
        checkedBy = randomUUID();
        checkedAt = ZonedDateTimes.toString(ZonedDateTime.now());
        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope("sjp.command.add-case-note",
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(CHECKED_BY, checkedBy.toString())
                        .add(CHECKED_AT, checkedAt)
                        .build()
        );

        legalAdviserDetails = buildLegalAdviserDetails();
        when(usersGroupsService.getUserDetails(any(), any(JsonEnvelope.class))).thenReturn(legalAdviserDetails);

        caseLegalSocCheckedProcessor.handleCaseLegalSocChecked(privateEvent);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(privateEvent)
                        .withName("sjp.command.add-case-note"), payloadIsJson(CoreMatchers.allOf()))));
    }

    private JsonObject buildLegalAdviserDetails() {
        return createObjectBuilder()
                .add("firstName", LEGAL_ADVISER_FIRST_NAME)
                .add("lastName", LEGAL_ADVISER_LAST_NAME)
                .build();
    }
}
