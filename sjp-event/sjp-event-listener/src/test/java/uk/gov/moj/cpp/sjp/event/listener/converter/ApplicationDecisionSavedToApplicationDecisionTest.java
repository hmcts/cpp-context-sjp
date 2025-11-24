package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationDecision.applicationDecision;
import static uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved.applicationDecisionSaved;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseApplicationRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationDecisionSavedToApplicationDecisionTest {



    @InjectMocks
    private ApplicationDecisionSavedToApplicationDecision converter;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private CaseApplicationRepository caseApplicationRepository;

    private static final UUID SESSION_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID DECISION_ID = randomUUID();
    private static final User SAVED_BY = new User("John", "Smith", randomUUID());
    private static final UUID CASE_ID = randomUUID();

    @Test
    public void shouldConvertApplicationDecisionSaved() {
        final ApplicationDecisionSaved applicationDecisionSaved = applicationDecisionSaved()
                .withSessionId(SESSION_ID)
                .withApplicationId(APPLICATION_ID)
                .withSavedAt(now())
                .withDecisionId(DECISION_ID)
                .withSavedBy(SAVED_BY)
                .withCaseId(CASE_ID)
                .withApplicationDecision(applicationDecision()
                        .withGranted(true)
                        .withOutOfTime(false)
                        .build())
                .build();

        when(sessionRepository.findBy(SESSION_ID)).thenReturn(new Session());
        when(caseApplicationRepository.findBy(APPLICATION_ID)).thenReturn(new CaseApplication());

        final CaseApplicationDecision applicationDecision = converter.convert(applicationDecisionSaved);
        assertThat(applicationDecision.getDecisionId(), equalTo(DECISION_ID));
        assertThat(applicationDecision.isGranted(), equalTo(true));
        assertThat(applicationDecision.getOutOfTime(), equalTo(false));
        assertThat(applicationDecision.getSession(), notNullValue());
        assertThat(applicationDecision.getSavedAt(), equalTo(applicationDecisionSaved.getSavedAt()));
        assertThat(applicationDecision.getCaseApplication(), notNullValue());
    }
}
