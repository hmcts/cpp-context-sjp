package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDocumentAddedToCaseDocumentTest {

    private CaseDocumentAddedToCaseDocument converter;
    private CaseDocumentAdded event;

    private Clock clock = new UtcClock();
    private UUID caseId = UUID.randomUUID();
    private UUID caseDocId = UUID.randomUUID();
    private UUID caseDocMaterialId = UUID.randomUUID();

    private static final String DOCUMENT_URI = "https://sadevfilestore.blob.core.windows.net/stack-stagingdvla/generated/sjpn.pdf";

    @BeforeEach
    public void setup() {
        CaseDocument caseDocument = new CaseDocument(caseDocId, caseDocMaterialId, "SJPN", clock.now(), DOCUMENT_URI);

        converter = new CaseDocumentAddedToCaseDocument();
        event = new CaseDocumentAdded(caseId, caseDocument, 1);
    }

    @Test
    public void shouldConvertCaseDocumentEventToCaseDocument() {
        uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument caseDocument = converter.convert(event);

        assertThat(caseDocument, is(notNullValue()));
        assertThat(caseDocument.getDocumentNumber(), is(1));
        assertThat(caseDocument.getDocumentUri(), is(DOCUMENT_URI));
    }

    @Test
    public void shouldLeaveDocumentUriNullForAFileServiceAddressedDocument() {
        final CaseDocument fileServiceAddressed = new CaseDocument(caseDocId, caseDocMaterialId, "SJPN", clock.now(), null);

        final uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument caseDocument =
                converter.convert(new CaseDocumentAdded(caseId, fileServiceAddressed, 1));

        assertThat(caseDocument.getDocumentUri(), is(nullValue()));
    }

}
