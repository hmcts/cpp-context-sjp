package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueNullMatcher.isJsonValueNull;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentQueryViewTest {

    private static final String CASE_ASSIGNMENT_QUERY_NAME = "sjp.query.case-assignment";

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private CaseDetail caseDetail;

    @Mock
    private CaseService caseService;

    @InjectMocks
    private AssignmentQueryView assignmentQueryView;

    private final UUID caseId = randomUUID();
    private final UUID userId = randomUUID();
    private final JsonEnvelope queryEnvelope = envelope()
            .with(metadataWithRandomUUID(CASE_ASSIGNMENT_QUERY_NAME).withUserId(userId.toString()))
            .withPayloadOf(caseId.toString(), "caseId")
            .build();

    @Test
    public void shouldReturnAssignmentWhenCaseIsAssignedToCallingUser() {
        when(caseDetail.getAssigneeId()).thenReturn(userId);
        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final JsonEnvelope assignmentResponse = assignmentQueryView.getCaseAssignment(queryEnvelope);

        assertAssignmentResponse(assignmentResponse, assignmentPayloadMatcher(true, true));
    }

    @Test
    public void shouldReturnAssignmentWhenCaseIsAssignedToOtherUser() {
        when(caseDetail.getAssigneeId()).thenReturn(randomUUID());
        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final JsonEnvelope assignmentResponse = assignmentQueryView.getCaseAssignment(queryEnvelope);

        assertAssignmentResponse(assignmentResponse, assignmentPayloadMatcher(true, false));
    }

    @Test
    public void shouldReturnAssignmentWhenCaseIsNotAssigned() {
        when(caseDetail.getAssigneeId()).thenReturn(null);
        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final JsonEnvelope assignmentResponse = assignmentQueryView.getCaseAssignment(queryEnvelope);

        assertAssignmentResponse(assignmentResponse, assignmentPayloadMatcher(false, false));
    }

    @Test
    public void shouldReturnNullResponseWhenCaseNotFound() {
        when(caseService.getCase(caseId)).thenReturn(Optional.empty());

        final JsonEnvelope assignmentResponse = assignmentQueryView.getCaseAssignment(queryEnvelope);

        assertAssignmentResponse(assignmentResponse, nullPayloadMatcher());
    }

    @Test
    public void shouldHandlesGetCaseAssignmentQuery() {
        assertThat(AssignmentQueryView.class, isHandlerClass(Component.QUERY_VIEW)
                .with(method("getCaseAssignment").thatHandles(CASE_ASSIGNMENT_QUERY_NAME)));
    }

    private void assertAssignmentResponse(final JsonEnvelope assignmentResponse, final JsonEnvelopePayloadMatcher payloadMatcher) {
        assertThat(assignmentResponse, jsonEnvelope(withMetadataEnvelopedFrom(queryEnvelope).withName(CASE_ASSIGNMENT_QUERY_NAME), payloadMatcher));
    }

    private static JsonEnvelopePayloadMatcher assignmentPayloadMatcher(final boolean assigned, final boolean assignedToMe) {
        return payload().isJson(allOf(
                withJsonPath("$.assigned", is(assigned)),
                withJsonPath("$.assignedToMe", is(assignedToMe))
        ));
    }

    private static JsonEnvelopePayloadMatcher nullPayloadMatcher() {
        return payload().isJsonValue(isJsonValueNull());
    }
}
