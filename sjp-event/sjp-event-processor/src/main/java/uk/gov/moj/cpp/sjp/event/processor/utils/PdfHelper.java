package uk.gov.moj.cpp.sjp.event.processor.utils;

import static org.apache.pdfbox.pdmodel.PDDocument.load;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfHelper {

    public int getDocumentPageCount(final byte[] pdfInBytes) throws IOException {
        final PDDocument pdfDocument = load(pdfInBytes);
        final int numberOfPages = pdfDocument.getNumberOfPages();

        pdfDocument.close();
        return numberOfPages;
    }
}
