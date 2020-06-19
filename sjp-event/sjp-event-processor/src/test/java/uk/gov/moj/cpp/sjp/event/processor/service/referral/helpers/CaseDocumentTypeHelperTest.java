package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDocumentTypeHelperTest {

    private static final String SJP_DOCUMENT_TYPE = "SJPN";
    private static final String SJP_OTHER_TYPE = "OTHER";
    private static final String SJP_FINANCIAL_MEANS = "FINANCIAL_MEANS";
    private static final String SJP_CITN = "CITN";
    private static final String SJP_PLEA_DOCUMENT_TYPE = "PLEA";
    private static final String SJP_INTENTION_TO_DISQUALIFY_NOTICE = "INTENTION_TO_DISQUALIFY_NOTICE";

    private static final String CC_PLEA = "Plea";
    private static final String CC_PRE_CONS = "Pre Cons";
    private static final String CC_SENTENCE = "Sentence";
    private static final String CC_DOCUMENT_TYPE = "Case Summary";
    private static final String CC_GENERAL_CORRESPONDENCE = "General correspondence";

    @Test
    public void shouldFindMatchingCriminalCourtDocumentType() {
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_DOCUMENT_TYPE), is(CC_DOCUMENT_TYPE));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_FINANCIAL_MEANS), is(CC_SENTENCE));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_CITN), is(CC_PRE_CONS));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_PLEA_DOCUMENT_TYPE), is(CC_PLEA));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_INTENTION_TO_DISQUALIFY_NOTICE), is(CC_SENTENCE));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_OTHER_TYPE), is(CC_GENERAL_CORRESPONDENCE));
    }
}
