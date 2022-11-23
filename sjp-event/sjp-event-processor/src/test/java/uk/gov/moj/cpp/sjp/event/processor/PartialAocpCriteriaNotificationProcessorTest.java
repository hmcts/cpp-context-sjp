package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.COURT_CENTRE_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.FULL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.SHORT_NAME;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.NotificationNotify;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PartialAocpCriteriaNotificationProcessorTest {
    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private NotificationNotify notificationNotify;

    @InjectMocks
    private PartialAocpCriteriaNotificationProcessor partialAocpCriteriaNotificationProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    private static final String TEMPLATE_ID = "3752de75-7ab4-4c4f-8a01-2a72aa1ea63d";


    @Before
    public void before() {
        // use defaults
        partialAocpCriteriaNotificationProcessor.replyToAddress = "noreply@cjscp.org.uk";
        partialAocpCriteriaNotificationProcessor.templateId = TEMPLATE_ID;
    }

    @Test
    public void shouldSendPleaNotificationEmail() {
        shouldSendPartialAocpCriteriaNotificationEmail(TEMPLATE_ID);
    }

    private void shouldSendPartialAocpCriteriaNotificationEmail(String templateId) {

        final UUID caseId = UUID.randomUUID();
        final String urn = "TFL123";
        final String prosecutingAuthority = "DVLA";

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("sjp.events.aocp-criteria-matched-partially"),
                createObjectBuilder()
                        .add("urn", urn)
                        .add("caseId", caseId.toString())
                        .add("prosecutingAuthority", prosecutingAuthority)
                        .build()
        );

      when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("policeFlag", true)
                .add("id", ID_2.toString())
                .add("shortName", SHORT_NAME)
                .add("fullName", FULL_NAME)
                .add("oucode", COURT_CENTRE_CODE)
                .add("contactEmailAddress", "prosecutor@prosecutor.com")
                .build())));

        partialAocpCriteriaNotificationProcessor.sendEmailToNotificationNotify(event);

        verify(sender, times(1)).send(argumentCaptor.capture());

        verify(notificationNotify, times(1)).sendEmail(any(), any());


    }

}
