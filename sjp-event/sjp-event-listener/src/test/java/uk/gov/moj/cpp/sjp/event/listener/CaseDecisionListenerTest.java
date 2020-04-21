package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.listener.converter.DecisionSavedToCaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.SetAsideOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDecisionListenerTest {

    private final UUID caseId = randomUUID();
    private final UUID decisionId = randomUUID();
    private final UUID offence1Id = randomUUID();
    private final UUID offence2Id = randomUUID();
    private final UUID offence3Id = randomUUID();

    private final ZonedDateTime now = ZonedDateTime.now();

    @InjectMocks
    private CaseDecisionListener listener;

    @Mock
    private Envelope<DecisionSaved> envelope;

    @Mock
    private DecisionSaved decisionSavedEvent;

    @Mock
    private DecisionSavedToCaseDecision eventConverter;

    @Mock
    private CaseDecisionRepository caseDecisionRepository;

    @Mock
    private CaseRepository caseRepository;

    @Test
    public void shouldEnrichOffenceDecisionsWithPleaInformation() {
        final OffenceDetail offence1Details = new OffenceDetail();
        offence1Details.setId(offence1Id);
        offence1Details.setSequenceNumber(1);
        offence1Details.setPlea(GUILTY);
        offence1Details.setPleaDate(now.minusDays(2));

        final OffenceDetail offence2Details = new OffenceDetail();
        offence2Details.setId(offence2Id);
        offence2Details.setSequenceNumber(2);
        offence2Details.setPlea(NOT_GUILTY);
        offence2Details.setPleaDate(now.minusDays(1));

        final OffenceDetail offence3Details = new OffenceDetail();
        offence3Details.setId(offence3Id);
        offence3Details.setSequenceNumber(3);

        final DefendantDetail defendantDetails = new DefendantDetail();
        defendantDetails.setOffences(asList(offence1Details, offence2Details, offence3Details));

        final CaseDetail caseDetails = new CaseDetail();
        caseDetails.setId(caseId);
        caseDetails.setDefendant(defendantDetails);

        final OffenceDecision offenceDecision1 = new DismissOffenceDecision(offence1Id, decisionId, NO_VERDICT);
        final OffenceDecision offenceDecision2 = new DismissOffenceDecision(offence2Id, decisionId, NO_VERDICT);
        final OffenceDecision offenceDecision3 = new DismissOffenceDecision(offence3Id, decisionId, NO_VERDICT);

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(decisionId);
        caseDecision.setCaseId(caseId);
        caseDecision.setOffenceDecisions(asList(offenceDecision1, offenceDecision2, offenceDecision3));

        when(envelope.payload()).thenReturn(decisionSavedEvent);
        when(eventConverter.convert(decisionSavedEvent)).thenReturn(caseDecision);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetails);

        listener.handleCaseDecisionSaved(envelope);

        assertThat(offenceDecision1.getPleaAtDecisionTime(), equalTo(offence1Details.getPlea()));
        assertThat(offenceDecision1.getPleaDate(), equalTo(offence1Details.getPleaDate()));

        assertThat(offenceDecision2.getPleaAtDecisionTime(), equalTo(offence2Details.getPlea()));
        assertThat(offenceDecision2.getPleaDate(), equalTo(offence2Details.getPleaDate()));

        assertThat(offenceDecision3.getPleaAtDecisionTime(), nullValue());
        assertThat(offenceDecision3.getPleaDate(), nullValue());

        verify(caseDecisionRepository).save(caseDecision);
    }

    @Test
    public void shouldSaveSetAsideOffenceDecision() {
        final OffenceDetail offence1Details = new OffenceDetail();
        offence1Details.setId(offence1Id);
        offence1Details.setSequenceNumber(1);
        offence1Details.setPlea(GUILTY);
        offence1Details.setPleaDate(now.minusDays(2));
        offence1Details.setConviction(FOUND_GUILTY);

        final OffenceDetail offence2Details = new OffenceDetail();
        offence2Details.setId(offence2Id);
        offence2Details.setSequenceNumber(2);
        offence2Details.setPlea(NOT_GUILTY);
        offence2Details.setPleaDate(now.minusDays(1));
        offence2Details.setConviction(FOUND_GUILTY);

        final OffenceDetail offence3Details = new OffenceDetail();
        offence3Details.setId(offence3Id);
        offence3Details.setSequenceNumber(3);
        offence3Details.setConviction(FOUND_GUILTY);

        final DefendantDetail defendantDetails = new DefendantDetail();
        defendantDetails.setOffences(asList(offence1Details, offence2Details, offence3Details));

        final CaseDetail caseDetails = new CaseDetail();
        caseDetails.setId(caseId);
        caseDetails.setDefendant(defendantDetails);

        final OffenceDecision offenceDecision1 = new SetAsideOffenceDecision(offence1Id, decisionId);
        final OffenceDecision offenceDecision2 = new SetAsideOffenceDecision(offence2Id, decisionId);
        final OffenceDecision offenceDecision3 = new SetAsideOffenceDecision(offence3Id, decisionId);

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(decisionId);
        caseDecision.setCaseId(caseId);
        caseDecision.setOffenceDecisions(asList(offenceDecision1, offenceDecision2, offenceDecision3));

        when(envelope.payload()).thenReturn(decisionSavedEvent);
        when(eventConverter.convert(decisionSavedEvent)).thenReturn(caseDecision);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetails);

        listener.handleCaseDecisionSaved(envelope);

        assertThat(caseDetails.getSetAside(), is(true));
        assertThat(caseDetails
                .getDefendant()
                .getOffences()
                .stream()
                .allMatch(offence -> offence.getConviction() == null), is(true));

        verify(caseDecisionRepository).save(caseDecision);
    }
}
