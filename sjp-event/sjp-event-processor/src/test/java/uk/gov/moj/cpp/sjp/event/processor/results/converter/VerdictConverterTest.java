package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT_3;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT_4;

import uk.gov.justice.core.courts.Verdict;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VerdictConverterTest {

    @InjectMocks
    VerdictConverter verdictConverter;

    @Mock
    ConvictionInfo convictionInfo;

    @Mock
    ReferenceDataService referenceDataService;

    @Spy
    @InjectMocks
    JCachedReferenceData jCachedReferenceData = new JCachedReferenceData(referenceDataService);


    @Test
    public void shouldConvertCaseDecision() {

        when(convictionInfo.getVerdictType()).thenReturn(VerdictType.FOUND_GUILTY);
        when(referenceDataService.getAllVerdictTypesByJurisdiction(any(), any())).thenReturn(allMagistrateJurisdictionTypeVerdictTypes());
        final Verdict verdict = verdictConverter.getVerdict(convictionInfo);

        assertThat(verdict, is(notNullValue()));
        assertThat(verdict.getVerdictType().getCjsVerdictCode(), is("GM"));
        assertThat(verdict.getVerdictType().getId(), is(ID_1));
        assertThat(verdict.getVerdictType().getCategory(), is(RANDOM_TEXT_1));
        assertThat(verdict.getVerdictType().getCategoryType(), is(RANDOM_TEXT_2));
        assertThat(verdict.getVerdictType().getVerdictCode(), is("G"));
    }

    private List<JsonObject> allVerdictTypes() {
        final JsonObject guiltyMagistratesJsonObject = Json.createObjectBuilder()
                .add(ID, ID_1.toString())
                .add("verdictCode", "G")
                .add("category", RANDOM_TEXT_1)
                .add("categoryType", RANDOM_TEXT_2)
                .add("cjsVerdictCode", "GM")
                .add("jurisdiction", "MAGISTRATES").build();
        final JsonObject guiltyCrownJsonObject = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add("verdictCode", "G")
                .add("category", RANDOM_TEXT_1)
                .add("categoryType", RANDOM_TEXT_2)
                .add("cjsVerdictCode", "GC")
                .add("jurisdiction", "CROWN").build();
        final JsonObject nonGuiltyMagistratesJsonObject = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add("verdictCode", "NG")
                .add("category", RANDOM_TEXT_1)
                .add("categoryType", RANDOM_TEXT_2)
                .add("cjsVerdictCode", "NGM")
                .add("jurisdiction", "MAGISTRATES").build();
        final JsonObject nonGuiltyCrownJsonObject = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add("verdictCode", "NG")
                .add("category", RANDOM_TEXT_1)
                .add("categoryType", RANDOM_TEXT_2)
                .add("cjsVerdictCode", "NGC")
                .add("jurisdiction", "CROWN").build();
        return Lists.newArrayList(guiltyMagistratesJsonObject, guiltyCrownJsonObject, nonGuiltyMagistratesJsonObject, nonGuiltyCrownJsonObject);
    }

    private List<JsonObject> allMagistrateJurisdictionTypeVerdictTypes() {
        final JsonObject guiltyMagistratesJsonObject = Json.createObjectBuilder()
                .add(ID, ID_1.toString())
                .add("verdictCode", "G")
                .add("category", RANDOM_TEXT_1)
                .add("categoryType", RANDOM_TEXT_2)
                .add("cjsVerdictCode", "GM")
                .add("jurisdiction", "MAGISTRATES").build();
        final JsonObject nonGuiltyMagistratesJsonObject = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add("verdictCode", "NG")
                .add("category", RANDOM_TEXT_1)
                .add("categoryType", RANDOM_TEXT_2)
                .add("cjsVerdictCode", "NGM")
                .add("jurisdiction", "MAGISTRATES").build();
        return Lists.newArrayList(guiltyMagistratesJsonObject, nonGuiltyMagistratesJsonObject);
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
