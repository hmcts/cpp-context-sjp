package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.IN_PROGRESS;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.casemanagement.CaseManagementStatusChanged;
import uk.gov.moj.cpp.sjp.event.casemanagement.UpdateCasesManagementStatus;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChangeCaseManagementStatusHandlerTest {

    public static final String CHANGE_CASE_MANAGEMENT_STATUS = "sjp.command.change-case-management-status";

    //public static final UUID CASE_MANAGEMENT_STATUS_STREAM_ID = fromString("ed028c79-031f-4da7-865b-53117af630b9");

    @InjectMocks
    private ChangeCaseManagementStatusHandler changeCaseManagementStatusHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream caseManagementStatusEventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            CaseManagementStatusChanged.class, UpdateCasesManagementStatus.class);

    @Test
    public void shouldInvokeChangeCaseManagementStatus() {
        assertThat(changeCaseManagementStatusHandler, isHandler(COMMAND_HANDLER)
                .with(method("changeCaseManagementStatus")
                        .thatHandles(CHANGE_CASE_MANAGEMENT_STATUS)
                ));
    }

    @Test
    public void shouldChangeCaseManagementStatus() throws EventStreamException {
        final UUID case1Id = UUID.randomUUID();

        final JsonEnvelope updateCaseManagementStatusCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(CHANGE_CASE_MANAGEMENT_STATUS), createObjectBuilder()
                                        .add("caseId", case1Id.toString())
                                        .add("caseManagementStatus", IN_PROGRESS.toString())
                        .build());

        CaseManagementStatusChanged event = new CaseManagementStatusChanged(case1Id, IN_PROGRESS);

        when(eventSource.getStreamById(case1Id))
                .thenReturn(caseManagementStatusEventStream);
        when(aggregateService.get(caseManagementStatusEventStream, CaseAggregate.class))
                .thenReturn(caseAggregate);
        when(caseAggregate.changeCaseManagementStatus(IN_PROGRESS))
                .thenReturn(Stream.of(event));

        changeCaseManagementStatusHandler.changeCaseManagementStatus(updateCaseManagementStatusCommandJsonEnvelope);

        verify(caseAggregate).changeCaseManagementStatus(IN_PROGRESS);
    }
}
