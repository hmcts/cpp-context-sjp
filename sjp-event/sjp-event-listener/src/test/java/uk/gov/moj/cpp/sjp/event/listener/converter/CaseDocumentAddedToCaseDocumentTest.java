package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDocumentAddedToCaseDocumentTest {

    private CaseDocumentAddedToCaseDocument converter;
    private CaseDocumentAdded event;

    private Clock clock = new UtcClock();
    private UUID caseId = UUID.randomUUID();
    private UUID caseDocId = UUID.randomUUID();
    private UUID caseDocMaterialId = UUID.randomUUID();

    @Before
    public void setup() {
        CaseDocument caseDocument = new CaseDocument(caseDocId, caseDocMaterialId, "SJPN", clock.now());

        converter = new CaseDocumentAddedToCaseDocument();
        event = new CaseDocumentAdded(caseId, caseDocument, 1);
    }

    @Test
    public void shouldConvertCaseDocumentEventToCaseDocument() {
        uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument caseDocument = converter.convert(event);

        assertThat(caseDocument, is(notNullValue()));
        assertThat(caseDocument.getDocumentNumber(), is(1));
    }

}
