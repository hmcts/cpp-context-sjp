package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDocumentTypeHelperTest {

    private static final String SJP_DOCUMENT_TYPE = "SJPN";
    private static final String CC_DOCUMENT_TYPE = "Case Summary";
    private static final String OTHER_TYPE = "OTHER";
    private static final String CC_DEFAULT_TYPE = "Private section - Court logs";

    private CaseDocumentTypeHelper caseDocumentTypeHelper = new CaseDocumentTypeHelper();

    @Test
    public void shouldFindMatchingCriminalCourtDocumentType() {
        assertThat(caseDocumentTypeHelper.getDocumentType(SJP_DOCUMENT_TYPE), is(CC_DOCUMENT_TYPE));
        assertThat(caseDocumentTypeHelper.getDocumentType(OTHER_TYPE), is(CC_DEFAULT_TYPE));
    }
}
