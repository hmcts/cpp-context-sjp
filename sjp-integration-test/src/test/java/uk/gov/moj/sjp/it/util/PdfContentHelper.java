package uk.gov.moj.sjp.it.util;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfContentHelper {

    private final PDDocument document;

    public PdfContentHelper(final byte[] pdfContent) throws IOException {
        document = PDDocument.load(pdfContent);
    }

    public String getTextContent() throws IOException {
        return new PDFTextStripper().getText(document);
    }

}
