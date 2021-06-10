package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper.SESSION_ID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper.STARTED_AT;
import static uk.gov.moj.cpp.sjp.event.processor.utils.FileUtil.getFileContentAsJson;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;

import org.junit.Before;

public abstract class BaseDecisionResultAggregatorTest {

    protected final UUID sessionId = UUID.randomUUID();
    protected final UUID offence1Id = randomUUID();
    protected final ZonedDateTime startedAt = ZonedDateTime.now();
    protected final JsonEnvelope sjpSessionEnvelope = mock(JsonEnvelope.class);
    protected final DecisionAggregate resultsAggregate = new DecisionAggregate();
    protected final ZonedDateTime resultedOn = ZonedDateTime.parse("2021-04-30T09:30:00.000Z");
    protected  JCachedReferenceData jCachedReferenceData;
    protected ReferenceDataService referenceDataService;

    @Before
    public void setUp() {
        when(sjpSessionEnvelope.payloadAsJsonObject()).thenReturn(
                createObjectBuilder()
                        .add(STARTED_AT, startedAt.toString())
                        .add(SESSION_ID, sessionId.toString())
                        .build());

        referenceDataService = mock(ReferenceDataService.class);
        when(referenceDataService.getAllResultDefinitions(any(JsonEnvelope.class), any(LocalDate.class)))
                .thenReturn(
                        (getFileContentAsJson("resultsconverter/referencedata.all-result-definitions.json", new HashMap<>())
                                .getJsonArray("resultDefinitions")));

        when(referenceDataService.getFixedList(any(JsonEnvelope.class)))
                .thenReturn((getFileContentAsJson("resultsconverter/referencedata.fixedlist.json", new HashMap<>())));

        this.jCachedReferenceData = new JCachedReferenceData(referenceDataService);
    }




}
