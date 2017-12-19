package uk.gov.moj.cpp.sjp.event.listener;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.OnlinePleaConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.Employer;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.EmployerRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.ZonedDateTime;
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

    @Mock
    private OnlinePleaRepository.EmployerOnlinePleaRepository onlinePleaRepository;

    @Mock
    private OnlinePleaConverter onlinePleaConverter;

    @Mock
    private OnlinePlea onlinePlea;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private DefendantDetail defendantDetail;

    @InjectMocks
    private EmployerListener employerListener = new EmployerListener();

    @Captor
    private ArgumentCaptor<Employer> captor;

    private Clock clock = new StoppedClock(ZonedDateTime.now());
    private ZonedDateTime now = clock.now();

    private EmployerUpdated stubEmployerUpdatedEvent(boolean updatedByOnlinePlea) {
        final UUID defendantId = UUID.randomUUID();
        final uk.gov.moj.cpp.sjp.domain.Employer employer = new uk.gov.moj.cpp.sjp.domain.Employer(defendantId,"Test",
                "123", "07777888999",
                new Address("street", "suburb", "town", "county", "AA1 2BB"));
        if (updatedByOnlinePlea) {
            return EmployerUpdated.createEventForOnlinePlea(defendantId, employer, now);
        }
        else {
            return EmployerUpdated.createEvent(defendantId, employer);
        }
    }

    @Test
    public void shouldSaveEmployerUpdatedEvent() {
        final EmployerUpdated employerUpdated = stubEmployerUpdatedEvent(false);

        final JsonObject payload = createObjectBuilder().build();
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, EmployerUpdated.class)).thenReturn(employerUpdated);
        when(defendantRepository.findBy(employerUpdated.getDefendantId())).thenReturn(defendantDetail);
        when(onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, employerUpdated)).thenReturn(onlinePlea);

        employerListener.updateEmployer(eventEnvelope);

        verify(employerRepository).save(captor.capture());
        verify(jsonObjectConverter, times(1)).convert(payload, EmployerUpdated.class);
        verify(defendantRepository, never()).findBy(employerUpdated.getDefendantId());
        verify(onlinePleaConverter, never()).convertToOnlinePleaEntity(defendantDetail, employerUpdated);
        verify(onlinePleaRepository, never()).saveOnlinePlea(eq(onlinePlea));
        final Employer employer = captor.getValue();
        assertThat(employer.getDefendantId(), equalTo(employerUpdated.getDefendantId()));
        assertThat(employer.getName(), equalTo(employerUpdated.getName()));
        assertThat(employer.getEmployeeReference(), equalTo(employerUpdated.getEmployeeReference()));
        assertThat(employer.getPhone(), equalTo(employerUpdated.getPhone()));
        assertThat(employer.getAddress1(), equalTo(employerUpdated.getAddress().getAddress1()));
        assertThat(employer.getAddress2(), equalTo(employerUpdated.getAddress().getAddress2()));
        assertThat(employer.getAddress3(), equalTo(employerUpdated.getAddress().getAddress3()));
        assertThat(employer.getAddress4(), equalTo(employerUpdated.getAddress().getAddress4()));
        assertThat(employer.getPostCode(), equalTo(employerUpdated.getAddress().getPostcode()));
    }

    @Test
    public void shouldSaveEmployerUpdatedEventForOnlinePlea() {
        final EmployerUpdated employerUpdated = stubEmployerUpdatedEvent(true);

        final JsonObject payload = createObjectBuilder().build();
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, EmployerUpdated.class)).thenReturn(employerUpdated);
        when(defendantRepository.findBy(employerUpdated.getDefendantId())).thenReturn(defendantDetail);
        when(onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, employerUpdated)).thenReturn(onlinePlea);

        employerListener.updateEmployer(eventEnvelope);

        verify(employerRepository).save(captor.capture());
        verify(jsonObjectConverter, times(1)).convert(payload, EmployerUpdated.class);
        verify(defendantRepository, times(1)).findBy(employerUpdated.getDefendantId());
        verify(onlinePleaConverter, times(1)).convertToOnlinePleaEntity(defendantDetail, employerUpdated);
        verify(onlinePleaRepository, times(1)).saveOnlinePlea(eq(onlinePlea));
        final Employer employer = captor.getValue();
        assertThat(employer.getDefendantId(), equalTo(employerUpdated.getDefendantId()));
        assertThat(employer.getName(), equalTo(employerUpdated.getName()));
        assertThat(employer.getEmployeeReference(), equalTo(employerUpdated.getEmployeeReference()));
        assertThat(employer.getPhone(), equalTo(employerUpdated.getPhone()));
        assertThat(employer.getAddress1(), equalTo(employerUpdated.getAddress().getAddress1()));
        assertThat(employer.getAddress2(), equalTo(employerUpdated.getAddress().getAddress2()));
        assertThat(employer.getAddress3(), equalTo(employerUpdated.getAddress().getAddress3()));
        assertThat(employer.getAddress4(), equalTo(employerUpdated.getAddress().getAddress4()));
        assertThat(employer.getPostCode(), equalTo(employerUpdated.getAddress().getPostcode()));
    }

    @Test
    public void shouldSaveEmptyEmployerUpdatedEvent() {
        final EmployerUpdated employerUpdated = EmployerUpdated.createEvent(UUID.randomUUID(),
                new uk.gov.moj.cpp.sjp.domain.Employer(null, null, null, null, null));

        final JsonObject payload = createObjectBuilder().build();
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, EmployerUpdated.class)).thenReturn(employerUpdated);
        when(defendantRepository.findBy(employerUpdated.getDefendantId())).thenReturn(defendantDetail);
        when(onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, employerUpdated)).thenReturn(onlinePlea);

        employerListener.updateEmployer(eventEnvelope);

        verify(employerRepository, times(1)).save(captor.capture());
        verify(jsonObjectConverter, times(1)).convert(payload, EmployerUpdated.class);
        verify(defendantRepository, never()).findBy(defendantDetail);
        verify(onlinePleaConverter, never()).convertToOnlinePleaEntity(eq(defendantDetail), eq(employerUpdated));
        verify(onlinePleaRepository, never()).saveOnlinePlea(eq(onlinePlea));
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

    @Test
    public void shouldDeleteEmployerIfExists() {

        final UUID defendantId = UUID.randomUUID();
        final JsonEnvelope deleteEmployerEvent = createDeleteEmployerEvent(defendantId);
        final Employer existingEmployer = new Employer();

        when(employerRepository.findBy(defendantId)).thenReturn(existingEmployer);

        employerListener.deleteEmployer(deleteEmployerEvent);

        verify(employerRepository).remove(argThat(is(existingEmployer)));
    }

    @Test
    public void shouldNotDeleteNonExistingEmployer() {

        final UUID defendantId = UUID.randomUUID();
        final JsonEnvelope deleteEmployerEvent = createDeleteEmployerEvent(defendantId);

        when(employerRepository.findBy(defendantId)).thenReturn(null);

        employerListener.deleteEmployer(deleteEmployerEvent);

        verify(employerRepository, never()).remove(argThat(any(Employer.class)));
    }

    private JsonEnvelope createDeleteEmployerEvent(final UUID defendantId) {
        return envelope()
                .with(metadataWithRandomUUID("sjp.events.employer-deleted"))
                .withPayloadOf(defendantId.toString(), "defendantId")
                .build();
    }

}
