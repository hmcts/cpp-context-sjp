package uk.gov.moj.cpp.sjp.event.listener;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.Employer;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.EmployerRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
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
    private DefendantRepository defendantRepository;

    @InjectMocks
    private EmployerListener employerListener = new EmployerListener();

    @Captor
    private ArgumentCaptor<Employer> captor;

    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaCaptor;

    private Clock clock = new UtcClock();
    private ZonedDateTime now = clock.now();
    private DefendantDetail defendantDetail;

    private static final UUID CASE_ID = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        defendantDetail = new DefendantDetail();
        defendantDetail.setCaseDetail(new CaseDetail());
        defendantDetail.getCaseDetail().setId(CASE_ID);
    }

    @Test
    public void shouldSaveEmployerUpdatedEvent() {
        final UUID defendantId = UUID.randomUUID();
        final uk.gov.moj.cpp.sjp.domain.Employer employer1 = new uk.gov.moj.cpp.sjp.domain.Employer(defendantId, "Test",
                "123", "07777888999",
                new Address("street", "suburb", "town", "county", "nation", "AA1 2BB"));
        final EmployerUpdated employerUpdated = EmployerUpdated.createEvent(defendantId, employer1);

        final JsonObject payload = createObjectBuilder().build();
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, EmployerUpdated.class)).thenReturn(employerUpdated);

        employerListener.updateEmployer(eventEnvelope);

        verify(employerRepository).save(captor.capture());
        verify(jsonObjectConverter).convert(payload, EmployerUpdated.class);
        verifyNoInteractions(defendantRepository);
        verifyNoInteractions(onlinePleaRepository);

        final Employer employer = captor.getValue();
        assertThat(employer.getDefendantId(), equalTo(employerUpdated.getDefendantId()));
        assertThat(employer.getName(), equalTo(employerUpdated.getName()));
        assertThat(employer.getEmployeeReference(), equalTo(employerUpdated.getEmployeeReference()));
        assertThat(employer.getPhone(), equalTo(employerUpdated.getPhone()));
        assertThat(employer.getAddress1(), equalTo(employerUpdated.getAddress().getAddress1()));
        assertThat(employer.getAddress2(), equalTo(employerUpdated.getAddress().getAddress2()));
        assertThat(employer.getAddress3(), equalTo(employerUpdated.getAddress().getAddress3()));
        assertThat(employer.getAddress4(), equalTo(employerUpdated.getAddress().getAddress4()));
        assertThat(employer.getAddress5(), equalTo(employerUpdated.getAddress().getAddress5()));
        assertThat(employer.getPostcode(), equalTo(employerUpdated.getAddress().getPostcode()));
    }

    @Test
    public void shouldSaveEmployerUpdatedEventForOnlinePlea() {
        final UUID defendantId = UUID.randomUUID();
        when(defendantRepository.findCaseIdByDefendantId(defendantId)).thenReturn(CASE_ID);

        final uk.gov.moj.cpp.sjp.domain.Employer employer1 = new uk.gov.moj.cpp.sjp.domain.Employer(defendantId, "Test",
                "123", "07777888999",
                new Address("street", "suburb", "town", "county", "nation", "AA1 2BB"));
        final EmployerUpdated employerUpdated = EmployerUpdated.createEventForOnlinePlea(defendantId, employer1, now);

        final JsonObject payload = createObjectBuilder().build();
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, EmployerUpdated.class)).thenReturn(employerUpdated);

        employerListener.updateEmployer(eventEnvelope);

        verify(employerRepository).save(captor.capture());
        verify(jsonObjectConverter).convert(payload, EmployerUpdated.class);
        verify(defendantRepository).findCaseIdByDefendantId(employerUpdated.getDefendantId());
        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());

        final Employer employer = captor.getValue();
        assertThat(employer.getDefendantId(), equalTo(employerUpdated.getDefendantId()));
        assertThat(employer.getName(), equalTo(employerUpdated.getName()));
        assertThat(employer.getEmployeeReference(), equalTo(employerUpdated.getEmployeeReference()));
        assertThat(employer.getPhone(), equalTo(employerUpdated.getPhone()));
        assertThat(employer.getAddress1(), equalTo(employerUpdated.getAddress().getAddress1()));
        assertThat(employer.getAddress2(), equalTo(employerUpdated.getAddress().getAddress2()));
        assertThat(employer.getAddress3(), equalTo(employerUpdated.getAddress().getAddress3()));
        assertThat(employer.getAddress4(), equalTo(employerUpdated.getAddress().getAddress4()));
        assertThat(employer.getAddress5(), equalTo(employerUpdated.getAddress().getAddress5()));
        assertThat(employer.getPostcode(), equalTo(employerUpdated.getAddress().getPostcode()));

        //check that OnlinePlea is constructed properly
        assertThat(onlinePleaCaptor.getValue().getCaseId(), equalTo(defendantDetail.getCaseDetail().getId()));
        assertThat(onlinePleaCaptor.getValue().getEmployer().getName(), equalTo(employerUpdated.getName()));
        assertThat(onlinePleaCaptor.getValue().getEmployer().getEmployeeReference(), equalTo(employerUpdated.getEmployeeReference()));
        assertThat(onlinePleaCaptor.getValue().getEmployer().getPhone(), equalTo(employerUpdated.getPhone()));
        assertThat(onlinePleaCaptor.getValue().getEmployer().getAddress().getAddress1(), equalTo(employerUpdated.getAddress().getAddress1()));
        assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), equalTo(employerUpdated.getUpdatedDate()));
    }

    @Test
    public void shouldSaveEmptyEmployerUpdatedEvent() {
        final EmployerUpdated employerUpdated = EmployerUpdated.createEvent(UUID.randomUUID(),
                new uk.gov.moj.cpp.sjp.domain.Employer(null, null, null, null, null));

        final JsonObject payload = createObjectBuilder().build();
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, EmployerUpdated.class)).thenReturn(employerUpdated);

        employerListener.updateEmployer(eventEnvelope);

        verify(employerRepository, times(1)).save(captor.capture());
        verify(jsonObjectConverter, times(1)).convert(payload, EmployerUpdated.class);
        verifyNoInteractions(defendantRepository);
        verifyNoInteractions(onlinePleaRepository);
        final Employer employer = captor.getValue();
        assertThat(employer.getDefendantId(), equalTo(employerUpdated.getDefendantId()));
        assertThat(employer.getName(), nullValue());
        assertThat(employer.getEmployeeReference(), nullValue());
        assertThat(employer.getPhone(), nullValue());
        assertThat(employer.getAddress1(), nullValue());
        assertThat(employer.getAddress2(), nullValue());
        assertThat(employer.getAddress3(), nullValue());
        assertThat(employer.getAddress4(), nullValue());
        assertThat(employer.getAddress5(), nullValue());
        assertThat(employer.getPostcode(), nullValue());
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

    private static JsonEnvelope createDeleteEmployerEvent(final UUID defendantId) {
        return envelope()
                .with(metadataWithRandomUUID("sjp.events.employer-deleted"))
                .withPayloadOf(defendantId.toString(), "defendantId")
                .build();
    }

}
