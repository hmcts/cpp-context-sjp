package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDocumentTypeHelperTest {

    private static final String SJP_DOCUMENT_TYPE = "SJPN";
    private static final String SJP_OTHER_TYPE = "OTHER";
    private static final String SJP_ELECTRONIC_NOTIFICATIONS = "ELECTRONIC_NOTIFICATIONS";
    private static final String SJP_FINANCIAL_MEANS = "FINANCIAL_MEANS";
    private static final String SJP_CITN = "CITN";
    private static final String SJP_PLEA_DOCUMENT_TYPE = "PLEA";
    private static final String SJP_INTENTION_TO_DISQUALIFY_NOTICE = "INTENTION_TO_DISQUALIFY_NOTICE";
    private static final String SJP_DISQUALIFICATION_NOTICE = "DISQUALIFICATION_REPLY_SLIP";
    private static final String SJP_APPLICATION = "APPLICATION";

    private static final String CC_PLEA = "Plea";
    private static final String CC_PRE_CONS = "Pre Cons";
    private static final String CC_SENTENCE = "Sentence";
    private static final String CC_DISQUALIFICATION_SENTENCE = "Sentence";
    private static final String CC_DOCUMENT_TYPE = "Case Summary";
    private static final String CC_GENERAL_CORRESPONDENCE = "General correspondence";
    private static final String CC_APPLICATION = "Applications";
    private static final String CC_ELECTRONIC_NOTIFICATIONS = "Electronic Notifications";

    @Test
    public void shouldFindMatchingCriminalCourtDocumentType() {
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_DOCUMENT_TYPE), is(CC_DOCUMENT_TYPE));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_FINANCIAL_MEANS), is(CC_SENTENCE));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_CITN), is(CC_PRE_CONS));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_PLEA_DOCUMENT_TYPE), is(CC_PLEA));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_INTENTION_TO_DISQUALIFY_NOTICE), is(CC_SENTENCE));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_DISQUALIFICATION_NOTICE), is(CC_DISQUALIFICATION_SENTENCE));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_OTHER_TYPE), is(CC_GENERAL_CORRESPONDENCE));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_APPLICATION), is(CC_APPLICATION));
        assertThat(CaseDocumentTypeHelper.getDocumentType(SJP_ELECTRONIC_NOTIFICATIONS), is(CC_ELECTRONIC_NOTIFICATIONS));
    }
}
