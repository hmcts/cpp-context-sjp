package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffenceUpdatedListenerTest {

    private UUID offenceId = UUID.randomUUID();
    private UUID caseId = UUID.randomUUID();
    private UUID personId = UUID.randomUUID();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private OffenceRepository offenceRepository;

    @Mock
    private CaseSearchResultRepository searchResultRepository;

    @InjectMocks
    private OffenceUpdatedListener listener;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private PleaUpdated pleaUpdatedEvent;

    @Mock
    private PleaCancelled pleaCancelled;

    @Mock
    private OffenceDetail offence;

    @Mock
    private DefendantDetail defendant;

    @Mock
    private JsonObject payload;

    @Mock
    private CaseSearchResult searchResult;

    @Test
    public void shouldUpdatePlea() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, PleaUpdated.class)).thenReturn(pleaUpdatedEvent);
        when(pleaUpdatedEvent.getPlea()).thenReturn(Plea.Type.GUILTY.toString());
        when(pleaUpdatedEvent.getOffenceId()).thenReturn(offenceId.toString());
        when(pleaUpdatedEvent.getPleaMethod()).thenReturn(PleaMethod.ONLINE);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(pleaUpdatedEvent.getCaseId()).thenReturn(caseId.toString());
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(defendant.getPersonId()).thenReturn(personId);
        when(searchResultRepository.findByCaseIdAndPersonId(caseId, personId)).thenReturn(asList(searchResult));

        listener.updatePlea(envelope);

        verify(offence).setPlea(pleaUpdatedEvent.getPlea());
        verify(offence).setPleaMethod(pleaUpdatedEvent.getPleaMethod());
        //TODO: should use a fixed clock for 100% test reliability
        verify(searchResult).setPleaDate(now());
    }

    @Test
    public void shouldCancelPlea() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, PleaCancelled.class)).thenReturn(pleaCancelled);
        when(pleaCancelled.getOffenceId()).thenReturn(offenceId.toString());
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(pleaCancelled.getCaseId()).thenReturn(caseId.toString());
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(defendant.getPersonId()).thenReturn(personId);
        when(searchResultRepository.findByCaseIdAndPersonId(caseId, personId)).thenReturn(asList(searchResult));

        listener.cancelPlea(envelope);

        verify(offence).setPlea(null);
        verify(offence).setPleaMethod(null);
        verify(searchResult).setPleaDate(null);
    }
}
