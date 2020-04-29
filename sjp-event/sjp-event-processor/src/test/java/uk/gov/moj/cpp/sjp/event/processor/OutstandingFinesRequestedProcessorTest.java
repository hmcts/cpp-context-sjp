package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequest;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequestsQueryResult;
import uk.gov.moj.cpp.sjp.event.OutstandingFinesRequested;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OutstandingFinesRequestedProcessorTest {

    public static final int outstandingFinesBatchSize = 5;
    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Mock
    private Requester requester;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @InjectMocks
    private OutstandingFinesRequestedProcessor outstandingFinesRequestedProcessor;

    private UUID caseId1 = randomUUID();
    private UUID caseId2 = randomUUID();
    private UUID defendantId1 = randomUUID();
    private UUID defendantId2 = randomUUID();

    @Before
    public void initMocks() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.outstandingFinesRequestedProcessor, "outstandingFinesBatchSize", outstandingFinesBatchSize);
    }


    @Test
    public void publicComputeOutstandingFinesRequested() {
        final JsonObject outstandingFinesQuery = mock(JsonObject.class);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("sjp.events.outstanding-fines-requested"),
                outstandingFinesQuery);

        final OutstandingFinesRequested outstandingFinesRequested = OutstandingFinesRequested.newBuilder()
                .withHearingDate(LocalDate.parse("2020-05-21"))
                .build();
        when(jsonObjectToObjectConverter.convert(outstandingFinesQuery, OutstandingFinesRequested.class)).thenReturn(outstandingFinesRequested);

        final JsonEnvelope defendantOutstandingFineRequestsQueryEnvelope = mock(JsonEnvelope.class);
        when(requester.request(envelopeArgumentCaptor.capture())).thenReturn(defendantOutstandingFineRequestsQueryEnvelope);
        final JsonObject courtBasedDefendantQueryJsonObject = mock(JsonObject.class);
        when(defendantOutstandingFineRequestsQueryEnvelope.payloadAsJsonObject()).thenReturn(courtBasedDefendantQueryJsonObject);
        when(jsonObjectToObjectConverter.convert(courtBasedDefendantQueryJsonObject, DefendantOutstandingFineRequestsQueryResult.class)).thenReturn(getDefendantInfoQueryResult());


        doNothing().when(sender).send(envelopeArgumentCaptor.capture());

        outstandingFinesRequestedProcessor.outstandingFinesRequested(event);

        final List<JsonEnvelope> allValues = envelopeArgumentCaptor.getAllValues();
        final JsonEnvelope defendantInfoQueryEnvelope = allValues.get(0);
        final JsonEnvelope requestOutstandingFinesEnvelope = allValues.get(1);

        assertThat(defendantInfoQueryEnvelope.metadata(), withMetadataEnvelopedFrom(event)
                .withName("sjp.query.outstanding-fine-requests"));
        assertThat(defendantInfoQueryEnvelope.payload(),
                JsonEnvelopePayloadMatcher.payloadIsJson(
                        allOf(
                                hasNoJsonPath("$.hearingDate")
                        )))
        ;
        assertThat(requestOutstandingFinesEnvelope.metadata(), withMetadataEnvelopedFrom(event)
                .withCausationIds()
                .withName("stagingenforcement.request-outstanding-fine"));
        assertThat(requestOutstandingFinesEnvelope.payload(),
                JsonEnvelopePayloadMatcher.payloadIsJson(
                        allOf(
                                withJsonPath("$.fineRequests.[0].defendantId", is(defendantId1.toString())),
                                withJsonPath("$.fineRequests.[0].caseId", is(caseId1.toString())),
                                withJsonPath("$.fineRequests.[1].defendantId", is(defendantId2.toString())),
                                withJsonPath("$.fineRequests.[1].caseId", is(caseId2.toString()))
                        )));
    }


    @Test
    public void publicComputeOutstandingFinesRequestedWithEmptyQuery() {
        final JsonObject outstandingFinesQuery = mock(JsonObject.class);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("sjp.events.outstanding-fines-requested"),
                outstandingFinesQuery);

        final JsonEnvelope defendantOutstandingFineRequestsQueryEnvelope = mock(JsonEnvelope.class);
        when(requester.request(envelopeArgumentCaptor.capture())).thenReturn(defendantOutstandingFineRequestsQueryEnvelope);
        final JsonObject courtBasedDefendantQueryJsonObject = mock(JsonObject.class);
        when(defendantOutstandingFineRequestsQueryEnvelope.payloadAsJsonObject()).thenReturn(courtBasedDefendantQueryJsonObject);

        outstandingFinesRequestedProcessor.outstandingFinesRequested(event);

        final List<JsonEnvelope> allValues = envelopeArgumentCaptor.getAllValues();
        final JsonEnvelope defendantInfoQueryEnvelope = allValues.get(0);

        assertThat(defendantInfoQueryEnvelope.metadata(), withMetadataEnvelopedFrom(event)
                .withName("sjp.query.outstanding-fine-requests"));
        assertThat(defendantInfoQueryEnvelope.payload(),
                JsonEnvelopePayloadMatcher.payloadIsJson(
                        allOf(
                                hasNoJsonPath("$.hearingDate")
                        )));
        verifyZeroInteractions(sender);
    }

    @Test
    public void publicComputeOutstandingFinesRequestedWithBatches() {

        final JsonObject outstandingFinesQuery = mock(JsonObject.class);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("sjp.events.outstanding-fines-requested"),
                outstandingFinesQuery);

        final OutstandingFinesRequested outstandingFinesRequested = OutstandingFinesRequested.newBuilder()
                .withHearingDate(LocalDate.parse("2020-05-21"))
                .build();
        when(jsonObjectToObjectConverter.convert(outstandingFinesQuery, OutstandingFinesRequested.class)).thenReturn(outstandingFinesRequested);

        final JsonEnvelope defendantOutstandingFineRequestsQueryEnvelope = mock(JsonEnvelope.class);
        when(requester.request(envelopeArgumentCaptor.capture())).thenReturn(defendantOutstandingFineRequestsQueryEnvelope);

        final JsonObject courtBasedDefendantQueryJsonObject = mock(JsonObject.class);
        when(defendantOutstandingFineRequestsQueryEnvelope.payloadAsJsonObject()).thenReturn(courtBasedDefendantQueryJsonObject);

        DefendantOutstandingFineRequestsQueryResult defendantInfoQueryResult = getRandomDefendantInfoQueryResult();
        when(jsonObjectToObjectConverter.convert(courtBasedDefendantQueryJsonObject, DefendantOutstandingFineRequestsQueryResult.class)).thenReturn(defendantInfoQueryResult);


        doNothing().when(sender).send(envelopeArgumentCaptor.capture());

        outstandingFinesRequestedProcessor.outstandingFinesRequested(event);

        final List<JsonEnvelope> allValues = envelopeArgumentCaptor.getAllValues();
        final JsonEnvelope defendantInfoQueryEnvelope = allValues.get(0);
        final JsonEnvelope requestOutstandingFinesEnvelope = allValues.get(1);

        int numberOfBatches = (int) Math.ceil(defendantInfoQueryResult.getDefendantDetails().size() / (float) outstandingFinesBatchSize) + 1;
        assertThat(allValues.size(), is(numberOfBatches));

        assertThat(defendantInfoQueryEnvelope.metadata(), withMetadataEnvelopedFrom(event)
                .withName("sjp.query.outstanding-fine-requests"));
        assertThat(defendantInfoQueryEnvelope.payload(),
                JsonEnvelopePayloadMatcher.payloadIsJson(
                        allOf(
                                hasNoJsonPath("$.hearingDate")
                        )));
        assertThat(requestOutstandingFinesEnvelope.metadata(), withMetadataEnvelopedFrom(event)
                .withCausationIds()
                .withName("stagingenforcement.request-outstanding-fine"));


    }

    private DefendantOutstandingFineRequestsQueryResult getDefendantInfoQueryResult() {
        return new DefendantOutstandingFineRequestsQueryResult(Arrays.asList(
                DefendantOutstandingFineRequest.newBuilder()
                        .withDefendantId(defendantId1)
                        .withCaseId(caseId1)
                        .build(),
                DefendantOutstandingFineRequest.newBuilder()
                        .withDefendantId(defendantId2)
                        .withCaseId(caseId2)
                        .build()
        ));
    }

    private DefendantOutstandingFineRequestsQueryResult getRandomDefendantInfoQueryResult() {
        final Integer size = INTEGER.next() % 100 + 10;
        final ArrayList<DefendantOutstandingFineRequest> defendantDetails = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            defendantDetails.add(
                    DefendantOutstandingFineRequest.newBuilder()
                            .withDefendantId(randomUUID())
                            .withCaseId(randomUUID())
                            .build());
        }
        return new DefendantOutstandingFineRequestsQueryResult(defendantDetails);
    }
}
