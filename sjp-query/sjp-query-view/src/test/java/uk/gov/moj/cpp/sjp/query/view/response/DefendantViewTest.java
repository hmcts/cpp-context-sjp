package uk.gov.moj.cpp.sjp.query.view.response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DefendantViewTest {

    private static final String INTERPRETER_LANG = "French";

    @Test
    public void shouldGetOffences() {
        DefendantView defendantView = new DefendantView(buildDefendantDetail());

        assertTrue(defendantView.getOffences().isEmpty());
        assertThat(defendantView.getInterpreter().getLanguage(), is(INTERPRETER_LANG));
    }

    private static DefendantDetail buildDefendantDetail() {
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(UUID.randomUUID());

        InterpreterDetail interpreterDetail = new InterpreterDetail();
        interpreterDetail.setLanguage(INTERPRETER_LANG);

        DefendantDetail defendantDetail = new DefendantDetail();
        defendantDetail.setCaseDetail(caseDetail);
        defendantDetail.setInterpreter(interpreterDetail);

        return defendantDetail;
    }
}
