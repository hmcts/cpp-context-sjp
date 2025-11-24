package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZonedDateTime.now;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.ApplicationOffencesResults;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.ApplicationOffenceResultsSaved;
import uk.gov.moj.cpp.sjp.event.ApplicationResultsRecorded;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseApplicationOffenceResultsTest extends CaseAggregateBaseTest {
    private static final String TEMPLATE_PAYLOAD_HEARING = "json/hearing-results-added-for-day.json";
    private static final String TEMPLATE_PAYLOAD_APPLICATION_HEARING_IN_AGGREGATE = "json/application-hearing-results-added-for-day.json";
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @InjectMocks
    private CaseAggregate caseAggregate;
    private CaseAggregateState caseAggregateState;
    @BeforeEach
    @Override
    public void setUp() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        caseAggregateState = new CaseAggregateState();
        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_APPLICATION_HEARING_IN_AGGREGATE);
        final Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        caseAggregateState.setApplicationResults(new ApplicationResultsRecorded(hearingObject, "2024-09-27", false, null, now()));
        setField(caseAggregate,"state", caseAggregateState);
        super.setUp();
    }

    @Test
    public void saveApplicationOffencesResults(){
        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_HEARING);
        final Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        final ApplicationOffencesResults applicationOffencesResults =
                new ApplicationOffencesResults(hearingObject, "2024-09-27", false, null, now());
        final Stream<Object> eventStream = caseAggregate.saveApplicationOffencesResults(applicationOffencesResults);
        final List<Object> events = eventStream.collect(toList());
        assertThat(events, hasSize(1));
        final ApplicationOffenceResultsSaved applicationOffenceResultsSaved = (ApplicationOffenceResultsSaved) events.get(0);
        assertThat(applicationOffenceResultsSaved.getHearing(), is(notNullValue()));
        assertThat(applicationOffenceResultsSaved.getHearing()
                .getCourtApplications().get(0)
                .getCourtApplicationCases().get(0)
                .getOffences().get(0).getJudicialResults(), is(notNullValue()));
        assertThat(applicationOffenceResultsSaved.getHearing()
                .getCourtApplications().get(0)
                .getCourtApplicationCases().get(0)
                .getOffences().get(0).getJudicialResults().get(0).getResultText(), is("FO - Fine\nFined £500.00"));
    }

    @Test
    public void shouldNotSaveApplicationOffencesResults_applicationResultsEmptyInState(){
        setField(caseAggregate,"state", new CaseAggregateState());
        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_HEARING);
        final Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        final ApplicationOffencesResults applicationOffencesResults =
                new ApplicationOffencesResults(hearingObject, "2024-09-27", false, null, now());
        final Stream<Object> eventStream = caseAggregate.saveApplicationOffencesResults(applicationOffencesResults);
        final List<Object> events = eventStream.collect(toList());
        assertThat(events, hasSize(0));
    }

    private static JsonObject getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = Json.createReader(new StringReader(request));
        return reader.readObject();
    }

}
