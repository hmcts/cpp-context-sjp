package uk.gov.moj.cpp.sjp.query.view;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseAssignmentRestriction;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseAssignmentRestrictionRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAssignmentRestrictionViewTest {

    private static final String PROSECUTING_AUTHORITY = "TFL";
    private static final List<String> EXCLUDE = singletonList("1234");
    private static final List<String> INCLUDE_ONLY = singletonList("9876");
    private static final ZonedDateTime DATE_TIME_CREATED = now();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    @Mock
    private CaseAssignmentRestrictionRepository caseAssignmentRestrictionRepository;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private CaseAssignmentRestrictionView caseAssignmentRestrictionView;

    @Test
    public void shouldGetCaseAssignmentRestriction() throws JsonProcessingException {

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.query.case-assignment-restriction"),
                createObjectBuilder().add("prosecutingAuthority", PROSECUTING_AUTHORITY).build());
        final CaseAssignmentRestriction caseAssignmentRestriction = new CaseAssignmentRestriction();
        caseAssignmentRestriction.setDateTimeCreated(DATE_TIME_CREATED);
        caseAssignmentRestriction.setProsecutingAuthority(PROSECUTING_AUTHORITY);
        caseAssignmentRestriction.setExclude(OBJECT_MAPPER.writeValueAsString(EXCLUDE));
        caseAssignmentRestriction.setIncludeOnly(OBJECT_MAPPER.writeValueAsString(INCLUDE_ONLY));
        caseAssignmentRestriction.setValidTo(LocalDate.now());
        caseAssignmentRestriction.setValidFrom(LocalDate.now());

        when(caseAssignmentRestrictionRepository.findByProsecutingAuthority(PROSECUTING_AUTHORITY, LocalDate.now())).thenReturn(List.of(caseAssignmentRestriction));

        final JsonObject restrictionResult = caseAssignmentRestrictionView.getCaseAssignmentRestriction(envelope).payloadAsJsonObject();
        assertThat(restrictionResult.getString("prosecutingAuthority"), equalTo(PROSECUTING_AUTHORITY));
        assertThat(restrictionResult.getString("dateTimeCreated"), equalTo(DATE_TIME_CREATED.toString()));
        assertThat(restrictionResult.getJsonArray("exclude")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(toList()), equalTo(EXCLUDE));
        assertThat(restrictionResult.getJsonArray("includeOnly")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(toList()), equalTo(INCLUDE_ONLY));
    }
}
