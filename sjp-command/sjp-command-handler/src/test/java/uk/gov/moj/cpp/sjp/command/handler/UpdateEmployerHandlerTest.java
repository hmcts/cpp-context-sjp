package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateEmployerHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private UpdateEmployerHandler updateEmployerHandler;

    @Test
    public void shouldUpdateEmployer() throws EventStreamException {

        final Employer employer = new Employer(UUID.randomUUID(), "Nando's", null, "0208123123",
                new Address("123 High St", null, null, null, "PC1 1CP"));

        when(converter.convert(jsonObject, Employer.class)).thenReturn(employer);
        when(caseAggregate.updateEmployer(employer)).thenReturn(events);

        updateEmployerHandler.updateEmployer(jsonEnvelope);

        verify(converter).convert(jsonObject, Employer.class);
        verify(caseAggregate).updateEmployer(employer);
    }
}