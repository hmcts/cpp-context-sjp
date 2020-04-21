package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpReadyCasesQueryViewTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private ReadyCaseRepository readyCaseRepository;

    @InjectMocks
    private SjpReadyCasesQueryView sjpReadyCasesQueryView;

    @Test
    public void shouldReturnAllReadyCases() {
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.ready-cases"))
                .build();

        final ReadyCase readyCase1 = new ReadyCase(randomUUID(), PIA, null, MAGISTRATE, 3, "TFL", now().minusDays(30));
        final ReadyCase readyCase2 = new ReadyCase(randomUUID(), PLEADED_GUILTY, randomUUID(), MAGISTRATE, 2, "TFL", now().minusDays(15));

        when(readyCaseRepository.findAll()).thenReturn(asList(readyCase1, readyCase2));

        final JsonEnvelope readyCases = sjpReadyCasesQueryView.getReadyCases(queryEnvelope);

        assertThat(readyCases, jsonEnvelope(metadata().withName("sjp.query.ready-cases"),
                payload().isJson(allOf(withJsonPath("readyCases", allOf(
                        hasItem(isJson(allOf(
                                withJsonPath("caseId", equalTo(readyCase1.getCaseId().toString())),
                                withJsonPath("reason", equalTo(readyCase1.getReason().name())),
                                withoutJsonPath("assigneeId")
                        ))),
                        hasItem(isJson(allOf(
                                withJsonPath("caseId", equalTo(readyCase2.getCaseId().toString())),
                                withJsonPath("reason", equalTo(readyCase2.getReason().name())),
                                withJsonPath("assigneeId", equalTo(readyCase2.getAssigneeId().get().toString()))
                        )))
                ))))));

        verify(readyCaseRepository, never()).findByAssigneeId(any());
    }

    @Test
    public void shouldReturnReadyCasesForAssignee() {
        final UUID assigneeId = UUID.randomUUID();
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.ready-cases")).withPayloadOf(assigneeId, "assigneeId")
                .build();

        final ReadyCase readyCase1 = new ReadyCase(randomUUID(), PIA, assigneeId, MAGISTRATE, 3, "TFL", now().minusDays(30));
        final ReadyCase readyCase2 = new ReadyCase(randomUUID(), PLEADED_GUILTY, assigneeId, MAGISTRATE, 2, "TFL", now().minusDays(15));

        when(readyCaseRepository.findByAssigneeId(assigneeId)).thenReturn(asList(readyCase1, readyCase2));

        final JsonEnvelope readyCases = sjpReadyCasesQueryView.getReadyCases(queryEnvelope);

        assertThat(readyCases, jsonEnvelope(metadata().withName("sjp.query.ready-cases"),
                payload().isJson(allOf(withJsonPath("readyCases", allOf(
                        hasItem(isJson(allOf(
                                withJsonPath("caseId", equalTo(readyCase1.getCaseId().toString())),
                                withJsonPath("reason", equalTo(readyCase1.getReason().name())),
                                withJsonPath("assigneeId", equalTo(assigneeId.toString()))
                        ))),
                        hasItem(isJson(allOf(
                                withJsonPath("caseId", equalTo(readyCase2.getCaseId().toString())),
                                withJsonPath("reason", equalTo(readyCase2.getReason().name())),
                                withJsonPath("assigneeId", equalTo(assigneeId.toString()))
                        )))
                ))))));

        verify(readyCaseRepository, never()).findAll();
    }

    @Test
    public void shouldReturnEmptyArrayWhenNoReadyCases() {
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.ready-cases"))
                .build();

        when(readyCaseRepository.findAll()).thenReturn(Collections.emptyList());

        final JsonEnvelope readyCases = sjpReadyCasesQueryView.getReadyCases(queryEnvelope);

        assertThat(readyCases, jsonEnvelope(metadata().withName("sjp.query.ready-cases"),
                payload().isJson(allOf(withJsonPath("readyCases", hasSize(0))))));
    }

}
