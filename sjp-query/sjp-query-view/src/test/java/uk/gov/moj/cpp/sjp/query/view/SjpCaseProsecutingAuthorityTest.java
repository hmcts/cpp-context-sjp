package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueNullMatcher.isJsonValueNull;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TVL;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpCaseProsecutingAuthorityTest {

    private static final String PROSECUTING_AUTHORITY_QUERY_NAME = "sjp.query.case-prosecuting-authority";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    final UUID caseId = randomUUID();

    final JsonEnvelope queryEnvelope = envelope()
            .with(metadataWithRandomUUID(PROSECUTING_AUTHORITY_QUERY_NAME))
            .withPayloadOf(caseId, "caseId")
            .build();

    @Test
    public void shouldGetCaseProsecutingAuthority() {
        final ProsecutingAuthority prosecutingAuthority = TVL;

        when(caseRepository.getProsecutingAuthority(caseId)).thenReturn(prosecutingAuthority.name());

        assertThat(sjpQueryView.getProsecutingAuthority(queryEnvelope), jsonEnvelope(
                metadata().withName(PROSECUTING_AUTHORITY_QUERY_NAME),
                payload().isJson(withJsonPath("$.prosecutingAuthority", equalTo(prosecutingAuthority.name())))
        ));
    }

    @Test
    public void shouldReturnNullJsonWhenNoProsecutingAuthorityFound() {
        when(caseRepository.getProsecutingAuthority(caseId)).thenReturn(null);

        assertThat(sjpQueryView.getProsecutingAuthority(queryEnvelope), jsonEnvelope(
                metadata().withName(PROSECUTING_AUTHORITY_QUERY_NAME),
                payload().isJsonValue(isJsonValueNull())
        ));
    }

}
