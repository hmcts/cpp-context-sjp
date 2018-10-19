package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;
import static uk.gov.moj.cpp.sjp.domain.plea.EmploymentStatus.EMPLOYED;

import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.DefendantNotEmployed;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class CaseEmployerHandler {

    public static final CaseEmployerHandler INSTANCE = new CaseEmployerHandler();

    private CaseEmployerHandler() {
    }

    public Stream<Object> updateEmployer(final UUID userId,
                                         final Employer employer,
                                         final CaseAggregateState state) {

        return createRejectionEvents(
                userId,
                "Update employer",
                employer.getDefendantId(),
                state
        ).orElse(getEmployerEventStream(employer, state));
    }

    public Stream<Object> deleteEmployer(final UUID userId,
                                         final UUID defendantId,
                                         final CaseAggregateState state) {

        return createRejectionEvents(
                userId,
                "Delete employer",
                defendantId,
                state)
                .orElse(
                        Stream.of(
                                state.getDefendantEmploymentStatus(defendantId).isPresent()
                                        ? new EmployerDeleted(defendantId)
                                        : new DefendantNotEmployed(defendantId)));
    }

    private Stream<Object> getEmployerEventStream(final Employer employer,
                                                  final CaseAggregateState state) {

        return getEmployerEventStream(employer, false, null, state);
    }

    private Stream<Object> getEmployerEventStream(final Employer employer,
                                                  final boolean updatedByOnlinePlea,
                                                  final ZonedDateTime createdOn,
                                                  final CaseAggregateState state) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final UUID defendantId = employer.getDefendantId();

        if (updatedByOnlinePlea) {
            streamBuilder.add(EmployerUpdated.createEventForOnlinePlea(defendantId, employer, createdOn));
        } else {
            streamBuilder.add(EmployerUpdated.createEvent(defendantId, employer));
        }

        final String actualEmploymentStatus = state.getDefendantEmploymentStatus(defendantId).orElse(null);

        if (!EMPLOYED.name().equals(actualEmploymentStatus)) {
            streamBuilder.add(new EmploymentStatusUpdated(defendantId, EMPLOYED.name()));
        }

        return streamBuilder.build();
    }

    public Stream<Object> getEmployerEventStream(final Employer employer,
                                                 final UUID defendantId,
                                                 final boolean updatedByOnlinePlea,
                                                 final ZonedDateTime createdOn,
                                                 final CaseAggregateState state) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (updatedByOnlinePlea) {
            streamBuilder.add(EmployerUpdated.createEventForOnlinePlea(defendantId, employer, createdOn));
        } else {
            streamBuilder.add(EmployerUpdated.createEvent(defendantId, employer));
        }

        final String actualEmploymentStatus = state.getDefendantEmploymentStatus(defendantId).orElse(null);

        if (!EMPLOYED.name().equals(actualEmploymentStatus)) {
            streamBuilder.add(new EmploymentStatusUpdated(defendantId, EMPLOYED.name()));
        }

        return streamBuilder.build();
    }

}
