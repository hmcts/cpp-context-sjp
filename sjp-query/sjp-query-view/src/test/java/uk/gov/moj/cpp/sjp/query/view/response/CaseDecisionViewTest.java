package uk.gov.moj.cpp.sjp.query.view.response;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;


import java.util.Arrays;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.util.ArrayList;

import org.junit.Test;

public class CaseDecisionViewTest {

    @Test
    public void shouldCreateCaseDecision() {

        final Session sessionEntity = buildSessionEntity();
        final CaseDecision entity = buildCaseDecisionEntity(sessionEntity);

        final CaseDecisionView view = new CaseDecisionView(entity);

        assertEquals(entity.getId(), view.getId());
        assertEquals(entity.getSavedAt(), view.getSavedAt());
        assertEquals(entity.getOffenceDecisions().size(), view.getOffenceDecisions().size());

        final SessionView sessionView = view.getSession();
        assertEquals(sessionEntity.getSessionId(), sessionView.getSessionId());
        assertEquals(sessionEntity.getUserId(), sessionView.getLegalAdviserUserId());
        assertEquals(sessionEntity.getCourtHouseCode(), sessionView.getCourtHouseCode());
        assertEquals(sessionEntity.getLocalJusticeAreaNationalCourtCode(), sessionView.getLocalJusticeAreaNationalCourtCode());
        assertEquals(sessionEntity.getMagistrate(), sessionView.getMagistrate());
        assertEquals(sessionEntity.getStartedAt(), sessionView.getStartedAt());
        assertEquals(sessionEntity.getEndedAt(), sessionView.getEndedAt());
        assertEquals(sessionEntity.getType().name(), sessionView.getSessionType());
    }

    private CaseDecision buildCaseDecisionEntity(final Session sessionEntity) {

        final CaseDecision entity = new CaseDecision();

        entity.setId(randomUUID());
        entity.setSavedAt(now());
        entity.setOffenceDecisions(new ArrayList<>());

        entity.setSession(sessionEntity);

        return entity;
    }

    private Session buildSessionEntity() {

        return new Session(
                randomUUID(),
                randomUUID(),
                "courtHouseCode",
                "courtHouseName",
                "localJusticeAreaNationalCourtCode",
                "magistrate",
                now(),
                Arrays.asList("TFL", "DVL")
        );
    }
}
