package uk.gov.moj.cpp.sjp.event.processor.utils;

import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class PdfHelperTest {

    private final PdfHelper pdfHelper = new PdfHelper();

    @Test
    public void shouldGetDocumentPageCount() throws IOException {
        final byte[] pdfDocumentInBytes = getFileInBytesFromName("scrooge-full.pdf");

        assertThat(pdfHelper.getDocumentPageCount(pdfDocumentInBytes), is(2));
    }

    private byte[] getFileInBytesFromName(final String fileName) throws IOException {
        final String path = "src/test/resources/documents/";
        return readFileToByteArray(new File(path.concat(fileName)));
    }
}
