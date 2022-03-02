package uk.gov.moj.cpp.sjp.event.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.Arrays;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

@RunWith(MockitoJUnitRunner.class)
public class CourtApplicationCreatedProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @InjectMocks
    private CourtApplicationCreatedProcessor processor;

    @Mock
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Mock
    private JsonObject payload ;


    @Mock
    private  JsonObject courtApplicationJsonObject;

    private static String APPEARANCE_TO_MAKE_STATUTORY_DECLARATION = "Appearance to make statutory declaration";
    private static String APPLICATION_TO_REOPEN_CASE = "Application to reopen case";
    private static String APPEAL = "Appeal";

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void processPublicEventCourtApplicationCreatedForSJPCaseWhenApplicationTypeIsAppearanceToMakeStatutoryDeclaration() {

        final UUID applicationId = randomUUID();
        final UUID sjpCaseId = randomUUID();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.court-application-created"),payload);
        final CourtApplication courtApplication = CourtApplication.courtApplication().
                withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION)
                        .withCode("MC80528")
                        .withAppealFlag(false)
                        .withLinkType(LinkType.LINKED)
                        .build())
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(sjpCaseId)
                        .withIsSJP(true)
                        .build()))
                .build();

        when(payload.getJsonObject("courtApplication")).thenReturn(courtApplicationJsonObject);
        when(jsonObjectToObjectConverter.convert(courtApplicationJsonObject, CourtApplication.class)).thenReturn(courtApplication);

        processor.courtApplicationCreated(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(),
                jsonEnvelope(metadata().withName("sjp.command.update-cc-case-application-status"), payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(sjpCaseId.toString())),
                        withJsonPath("$.applicationId", is(applicationId.toString())),
                        withJsonPath("$.applicationStatus", is(ApplicationStatus.STATUTORY_DECLARATION_PENDING.name()))))));


    }

    @Test
    public void processPublicEventCourtApplicationCreatedForSJPCaseWhenApplicationTypeIsAppealToReopenCase() {

        final UUID applicationId = randomUUID();
        final UUID sjpCaseId = randomUUID();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.court-application-created"),payload);
        final CourtApplication courtApplication = CourtApplication.courtApplication().
                withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPLICATION_TO_REOPEN_CASE)
                        .withCode("MC80524")
                        .withAppealFlag(false)
                        .withLinkType(LinkType.LINKED)
                        .build())
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(sjpCaseId)
                        .withIsSJP(true)
                        .build()))
                .build();

        when(payload.getJsonObject("courtApplication")).thenReturn(courtApplicationJsonObject);
        when(jsonObjectToObjectConverter.convert(courtApplicationJsonObject, CourtApplication.class)).thenReturn(courtApplication);

        processor.courtApplicationCreated(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(),
                jsonEnvelope(metadata().withName("sjp.command.update-cc-case-application-status"), payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(sjpCaseId.toString())),
                        withJsonPath("$.applicationId", is(applicationId.toString())),
                        withJsonPath("$.applicationStatus", is(ApplicationStatus.REOPENING_PENDING.name()))))));


    }

    @Test
    public void processPublicEventCourtApplicationCreatedForSJPCaseWhenApplicationTypeIsAppeal() {

        final UUID applicationId = randomUUID();
        final UUID sjpCaseId = randomUUID();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.court-application-created"),payload);
        final CourtApplication courtApplication = CourtApplication.courtApplication().
                withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPEAL)
                        .withAppealFlag(true)
                        .withLinkType(LinkType.SJP)
                        .build())
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(sjpCaseId)
                        .withIsSJP(true)
                        .build()))
                .build();

        when(payload.getJsonObject("courtApplication")).thenReturn(courtApplicationJsonObject);
        when(jsonObjectToObjectConverter.convert(courtApplicationJsonObject, CourtApplication.class)).thenReturn(courtApplication);

        processor.courtApplicationCreated(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(),
                jsonEnvelope(metadata().withName("sjp.command.update-cc-case-application-status"), payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(sjpCaseId.toString())),
                        withJsonPath("$.applicationId", is(applicationId.toString())),
                        withJsonPath("$.applicationStatus", is(ApplicationStatus.APPEAL_PENDING.name()))))));


    }

    @Test
    public void processPublicEventCourtApplicationCreatedForSJPCaseWhenApplicationTypeIsNotLinkedWithSJPCase() {

        final UUID applicationId = randomUUID();
        final UUID sjpCaseId = randomUUID();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.court-application-created"),payload);
        final CourtApplication courtApplication = CourtApplication.courtApplication().
                withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType("Non SJP Case")
                        .withAppealFlag(false)
                        .withLinkType(LinkType.LINKED)
                        .build())
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(sjpCaseId)
                        .withIsSJP(true)
                        .build()))
                .build();

        when(payload.getJsonObject("courtApplication")).thenReturn(courtApplicationJsonObject);
        when(jsonObjectToObjectConverter.convert(courtApplicationJsonObject, CourtApplication.class)).thenReturn(courtApplication);

        processor.courtApplicationCreated(event);

        verify(sender, times(0)).send(envelopeArgumentCaptor.capture());

    }

    @Test
    public void processPublicEventCourtApplicationCreatedForNonSJPCase() {

        final UUID applicationId = randomUUID();
        final UUID sjpCaseId = randomUUID();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.court-application-created"),payload);
        final CourtApplication courtApplication = CourtApplication.courtApplication().
                withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPEAL)
                        .withAppealFlag(true)
                        .withLinkType(LinkType.SJP)
                        .build())
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(sjpCaseId)
                        .withIsSJP(false)
                        .build()))
                .build();

        when(payload.getJsonObject("courtApplication")).thenReturn(courtApplicationJsonObject);
        when(jsonObjectToObjectConverter.convert(courtApplicationJsonObject, CourtApplication.class)).thenReturn(courtApplication);

        processor.courtApplicationCreated(event);

        verify(sender, times(0)).send(envelopeArgumentCaptor.capture());

    }

    @Test
    public void processPublicEventCourtApplicationCreatedWithoutCase() {

        final UUID applicationId = randomUUID();
        final UUID sjpCaseId = randomUUID();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.court-application-created"),payload);
        final CourtApplication courtApplication = CourtApplication.courtApplication().
                withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPEAL)
                        .withAppealFlag(true)
                        .withLinkType(LinkType.SJP)
                        .build())
                .build();

        when(payload.getJsonObject("courtApplication")).thenReturn(courtApplicationJsonObject);
        when(jsonObjectToObjectConverter.convert(courtApplicationJsonObject, CourtApplication.class)).thenReturn(courtApplication);

        processor.courtApplicationCreated(event);

        verify(sender, times(0)).send(envelopeArgumentCaptor.capture());

    }

}
