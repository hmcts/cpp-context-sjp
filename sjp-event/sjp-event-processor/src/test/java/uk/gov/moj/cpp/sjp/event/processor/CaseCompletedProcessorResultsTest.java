package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor.utils.FileUtil.getFileContentAsJson;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CaseCompletedProcessorResultsTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String CASE_RESULTS = "sjp.query.case-results";
    private static final String CASE_COMPLETED = "sjp.events.case-completed";

    @InjectMocks
    private CaseCompletedProcessor caseCompletedProcessor;

    @Mock
    private Requester requester;

    @Mock
    private Sender sender;

    @Mock
    private SjpService sjpService;

    @Mock
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<Envelope<PublicSjpResulted>> publicSjpResultedEnvelope;

    private JsonObjectToObjectConverter jsonObjectToObjectConverter = null;

    @Before
    public void setUp() {

        jsonObjectToObjectConverter = new JsonObjectToObjectConverter();
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(caseCompletedProcessor, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);

    }

    @Test
    public void shouldRaiseTwoPublicSjpResultedEventsWhenTwoCaseDecisionsAreMade() {
        // given
        // case completed envelope
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());

        // case results
        final JsonEnvelope caseResultsResponse = envelopeFrom(metadataWithRandomUUID(CASE_RESULTS),
                getFileContentAsJson("CaseCompletedProcessorTest/sjp.query.case-results-it.json", new HashMap<>()));

        // query case
        final JsonObject caseDetailsJson = getFileContentAsJson("CaseCompletedProcessorTest/sjp.query.case-it.json", new HashMap<>());
        final CaseDetails caseDetails = jsonObjectToObjectConverter.convert(caseDetailsJson, CaseDetails.class);

        when(sjpService.getCaseDetails(any(), any())).thenReturn(caseDetails);
        when(requester.request(any())).thenReturn(caseResultsResponse);


        // when
        caseCompletedProcessor.handleCaseCompleted(envelope);

        // then
        verify(sender,times(1)).send(publicSjpResultedEnvelope.capture());

        final List<Envelope<PublicSjpResulted>> envelopes = publicSjpResultedEnvelope.getAllValues();
        assertThat(envelopes.get(0).metadata().name(), is("sjp.command.request-delete-docs"));
    }

}
