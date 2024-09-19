package uk.gov.moj.cpp.sjp.query.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CaseDocumentViewTest {

    @Test
    public void testOrder() {
        //given
        CaseDocumentView caseDocumentViewA2 = new CaseDocumentView(
                new CaseDocument(null, null, "A-documentType", null, null, 2));
        CaseDocumentView caseDocumentViewB1 = new CaseDocumentView(
                new CaseDocument(null, null, "B-documentType", null, null, 1));
        CaseDocumentView caseDocumentViewB2 = new CaseDocumentView(
                new CaseDocument(null, null, "B-documentType", null, null, 2));

        List<CaseDocumentView> caseDocumentViews = new ArrayList<>();
        caseDocumentViews.add(caseDocumentViewA2);
        caseDocumentViews.add(caseDocumentViewB2);
        caseDocumentViews.add(caseDocumentViewB1);

        //when
        caseDocumentViews.sort(CaseDocumentView.BY_DOCUMENT_TYPE_AND_NUMBER);

        //then
        assertEquals("A-documentType", caseDocumentViews.get(0).getDocumentType());
        assertEquals("B-documentType", caseDocumentViews.get(1).getDocumentType());
        assertEquals(1, (int) caseDocumentViews.get(1).getDocumentNumber());
        assertEquals("B-documentType", caseDocumentViews.get(2).getDocumentType());
        assertEquals(2, (int) caseDocumentViews.get(2).getDocumentNumber());
    }

    @Test
    public void exceptionIsNotThrownWhenDocumentTypeIsNotDefined() {
        //given
        CaseDocumentView caseDocumentView1 = new CaseDocumentView(new CaseDocument());
        CaseDocumentView caseDocumentView2 = new CaseDocumentView(new CaseDocument());

        List<CaseDocumentView> caseDocumentViews = new ArrayList<>();
        caseDocumentViews.add(caseDocumentView1);
        caseDocumentViews.add(caseDocumentView2);

        //when
        caseDocumentViews.sort(CaseDocumentView.BY_DOCUMENT_TYPE_AND_NUMBER);

        //then test finishes successfully without any exceptions
    }
}
