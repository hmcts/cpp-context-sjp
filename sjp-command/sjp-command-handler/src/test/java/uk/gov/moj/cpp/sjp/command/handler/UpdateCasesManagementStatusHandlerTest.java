package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
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
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseManagementStatusAggregate;
import uk.gov.moj.cpp.sjp.domain.common.CaseByManagementStatus;
import uk.gov.moj.cpp.sjp.event.casemanagement.UpdateCasesManagementStatus;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateCasesManagementStatusHandlerTest {

    //public static final String CASE_MANAGEMENT_STATUS = "sjp.command.case-management-status";
    public static final String UPDATE_CASES_MANAGEMENT_STATUS = "sjp.command.update-cases-management-status";

    public static final UUID CASE_MANAGEMENT_STATUS_STREAM_ID = fromString("ed028c79-031f-4da7-865b-53117af630b9");

    @InjectMocks
    private UpdateCasesManagementStatusHandler updateCasesManagementStatusHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream caseManagementStatusEventStream;

    @Mock
    private CaseManagementStatusAggregate caseManagementStatusAggregate;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(UpdateCasesManagementStatus.class);

    @Test
    public void shouldUpdateCasesManagementStatusCommand() {
        assertThat(updateCasesManagementStatusHandler, isHandler(COMMAND_HANDLER)
                .with(method("updateCasesManagementStatus")
                        .thatHandles(UPDATE_CASES_MANAGEMENT_STATUS)
                ));
    }

    @Test
    public void shouldUpdateCaseManagementStatus() throws EventStreamException {
        final UUID case1Id = UUID.randomUUID();

        final JsonEnvelope caseManagementStatusCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(UPDATE_CASES_MANAGEMENT_STATUS),
                createObjectBuilder().add("cases", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("caseId", case1Id.toString())
                                .add("caseManagementStatus", IN_PROGRESS.toString()))
                ).build());

        final List<CaseByManagementStatus> caseByManagementStatusList = Arrays.asList(new CaseByManagementStatus(case1Id, IN_PROGRESS));

        final UpdateCasesManagementStatus updateCasesManagementStatus = new UpdateCasesManagementStatus(caseByManagementStatusList);

        when(eventSource.getStreamById(CASE_MANAGEMENT_STATUS_STREAM_ID))
                .thenReturn(caseManagementStatusEventStream);
        when(aggregateService.get(caseManagementStatusEventStream, CaseManagementStatusAggregate.class))
                .thenReturn(caseManagementStatusAggregate);
        when(caseManagementStatusAggregate.updateCaseManagementStatus(caseManagementStatusCommandJsonEnvelope.payloadAsJsonObject()))
                .thenReturn(Stream.of(updateCasesManagementStatus));

        updateCasesManagementStatusHandler.updateCasesManagementStatus(caseManagementStatusCommandJsonEnvelope);

        verify(caseManagementStatusAggregate).updateCaseManagementStatus(caseManagementStatusCommandJsonEnvelope.payloadAsJsonObject());
    }
}
