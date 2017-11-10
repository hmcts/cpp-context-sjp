package uk.gov.moj.cpp.sjp.query.view.response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class DefendantViewTest {

    final UUID id = UUID.randomUUID();
    final UUID caseId = UUID.randomUUID();
    final UUID personId = UUID.randomUUID();
    final Set<OffenceDetail> offences = new HashSet<>();
    final CaseDetail caseDetail = new CaseDetail();
    final InterpreterDetail interpreterDetail = new InterpreterDetail();

    @Before
    public void setup() {
        caseDetail.setId(caseId);
        interpreterDetail.setLanguage("French");
    }

    @Test
    public void shouldGetPersonId() {

        final DefendantDetail defendantDetail = getDefendantDetail();

        final DefendantView defendantView = new DefendantView(defendantDetail);

        assertThat(defendantView.getPersonId(), equalTo(personId));
    }

    @Test
    public void shouldGetOffences() {

        final DefendantDetail defendantDetail = getDefendantDetail();

        final DefendantView defendantView = new DefendantView(defendantDetail);

        assertTrue(defendantView.getOffences().isEmpty());
    }

    @Test
    public void shouldGetInterpreter() {

        final DefendantDetail defendantDetail = getDefendantDetail();
        defendantDetail.setInterpreter(interpreterDetail);

        final DefendantView defendantViewWithInterpreter = new DefendantView(defendantDetail);

        assertThat(defendantViewWithInterpreter.getInterpreter().getLanguage(),
                equalTo(interpreterDetail.getLanguage()));
    }

    private DefendantDetail getDefendantDetail() {
        final DefendantDetail defendantDetail = new DefendantDetail(id, personId, offences);
        defendantDetail.setCaseDetail(caseDetail);
        return defendantDetail;
    }
}
