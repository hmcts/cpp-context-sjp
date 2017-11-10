package uk.gov.moj.cpp.sjp.event.listener;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.Employer;
import uk.gov.moj.cpp.sjp.persistence.repository.EmployerRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class EmployerListenerTest {

    @Mock
    private JsonEnvelope eventEnvelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private EmployerRepository employerRepository;

    @InjectMocks
    private EmployerListener employerListener = new EmployerListener();

    @Captor
    private ArgumentCaptor<Employer> captor;

    @Test
    public void shouldSaveEmployerUpdatedEvent() {

        final EmployerUpdated employerUpdated = new EmployerUpdated(UUID.randomUUID(), "Test",
                "123", "07777888999", new Address("street",
                "suburb", "town", "county", "AA1 2BB"));

        final JsonObject payload = createObjectBuilder().build();
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, EmployerUpdated.class)).thenReturn(employerUpdated);

        employerListener.updateEmployer(eventEnvelope);

        verify(employerRepository).save(captor.capture());
        final Employer employer = captor.getValue();
        assertThat(employer.getDefendantId(), equalTo(employerUpdated.getDefendantId()));
        assertThat(employer.getName(), equalTo(employerUpdated.getName()));
        assertThat(employer.getEmployeeReference(), equalTo(employerUpdated.getEmployeeReference()));
        assertThat(employer.getPhone(), equalTo(employerUpdated.getPhone()));
        assertThat(employer.getAddress1(), equalTo(employerUpdated.getAddress().getAddress1()));
        assertThat(employer.getAddress2(), equalTo(employerUpdated.getAddress().getAddress2()));
        assertThat(employer.getAddress3(), equalTo(employerUpdated.getAddress().getAddress3()));
        assertThat(employer.getAddress4(), equalTo(employerUpdated.getAddress().getAddress4()));
        assertThat(employer.getPostCode(), equalTo(employerUpdated.getAddress().getPostCode()));
    }

    @Test
    public void shouldSaveEmptyEmployerUpdatedEvent() {

        final EmployerUpdated employerUpdated = new EmployerUpdated(UUID.randomUUID(), null,
                null, null, null);

        final JsonObject payload = createObjectBuilder().build();
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, EmployerUpdated.class)).thenReturn(employerUpdated);

        employerListener.updateEmployer(eventEnvelope);

        verify(employerRepository).save(captor.capture());
        final Employer employer = captor.getValue();
        assertThat(employer.getDefendantId(), equalTo(employerUpdated.getDefendantId()));
        assertThat(employer.getName(), nullValue());
        assertThat(employer.getEmployeeReference(), nullValue());
        assertThat(employer.getPhone(), nullValue());
        assertThat(employer.getAddress1(), nullValue());
        assertThat(employer.getAddress2(), nullValue());
        assertThat(employer.getAddress3(), nullValue());
        assertThat(employer.getAddress4(), nullValue());
        assertThat(employer.getPostCode(), nullValue());
    }

}
