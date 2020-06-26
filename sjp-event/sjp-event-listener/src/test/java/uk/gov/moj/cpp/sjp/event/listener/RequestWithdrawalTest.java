package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestWithdrawalTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseSearchResultRepository searchResultRepository;

    @InjectMocks
    private CaseUpdatedListener listener;

    @Mock
    private CaseSearchResult caseSearchResult;

    private UUID caseId;

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {

        this.caseId = UUID.randomUUID();

        final DefendantDetail defendantDetail = new DefendantDetail();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setDefendant(defendantDetail);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        when(searchResultRepository.findByCaseId(caseId))
                .thenReturn(asList(caseSearchResult));

        // Super class because the converter is modified by mockito so it does not have this field
        final Class<?> aClass = jsonObjectToObjectConverter.getClass().getSuperclass();

        final Field objectMapperField = aClass.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);

        objectMapperField.set(jsonObjectToObjectConverter, new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleProperEvents() throws NoSuchMethodException {
        final Class<CaseUpdatedListener> caseUpdatedListenerClass = CaseUpdatedListener.class;

        Handles handles;
        Method method;

        method = caseUpdatedListenerClass.getDeclaredMethod("allOffencesWithdrawalRequestCancelled", JsonEnvelope.class);
        handles = method.getAnnotation(Handles.class);

        assertThat("sjp.events.all-offences-withdrawal-request-cancelled", equalTo(handles.value()));

        method = caseUpdatedListenerClass.getDeclaredMethod("allOffencesWithdrawalRequested", JsonEnvelope.class);
        handles = method.getAnnotation(Handles.class);

        assertThat("sjp.events.all-offences-withdrawal-requested", equalTo(handles.value()));
    }

}
