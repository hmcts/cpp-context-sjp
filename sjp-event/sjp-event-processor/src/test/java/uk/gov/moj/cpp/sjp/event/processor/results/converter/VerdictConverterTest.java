package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT_3;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT_4;

import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;

import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VerdictConverterTest {

    @InjectMocks
    VerdictConverter verdictConverter;

    @Mock
    ConvictionInfo convictionInfo;


    @Mock
    JCachedReferenceData jCachedReferenceData;

    @Test
    public void shouldConvertCaseDecision() {

        Metadata metadata = metadataWithRandomUUID(RANDOM_TEXT).build();
        when(convictionInfo.getVerdictType()).thenReturn(VerdictType.FOUND_GUILTY);
        when(jCachedReferenceData.getVerdict(anyObject(), anyObject())).thenReturn(getVerdict());
        final Verdict verdict = verdictConverter.getVerdict(convictionInfo);

        assertThat(verdict, is(notNullValue()));
        assertThat(verdict.getVerdictType().getId(), is(ID_1));
        assertThat(verdict.getVerdictType().getCategory(), is(RANDOM_TEXT_1));
        assertThat(verdict.getVerdictType().getCategoryType(), is(RANDOM_TEXT_2));
        assertThat(verdict.getVerdictType().getVerdictCode(), is(RANDOM_TEXT_3));
        assertThat(verdict.getVerdictType().getCjsVerdictCode(), is(RANDOM_TEXT_4));

    }

    public static Optional<JsonObject> getVerdict() {
        return Optional.of(Json.createObjectBuilder()
                .add(ID, ID_1.toString())
                .add("category", RANDOM_TEXT_1)
                .add("categoryType", RANDOM_TEXT_2)
                .add("verdictCode", RANDOM_TEXT_3)
                .add("cjsVerdictCode", RANDOM_TEXT_4)
                .build());
    }

}
