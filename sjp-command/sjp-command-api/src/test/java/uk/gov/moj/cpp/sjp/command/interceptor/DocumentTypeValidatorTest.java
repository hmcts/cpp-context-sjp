package uk.gov.moj.cpp.sjp.command.interceptor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("squid:S2187")
@RunWith(MockitoJUnitRunner.class)
public class DocumentTypeValidatorTest extends TestCase {

    @InjectMocks
    DocumentTypeValidator documentTypeValidator;

    @Test
    public void shouldFailValidationForNullFileName() {
        final boolean valid = documentTypeValidator.isValid(null);
        assertThat(valid, is(false));
    }

    @Test
    public void shouldFailValidationForEmptyFileName() {
        final boolean valid = documentTypeValidator.isValid("");
        assertThat(valid, is(false));
    }

    @Test
    public void shouldFailValidationForIncorrectFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.exe");
        assertThat(valid, is(false));
    }

    @Test
    public void shouldPassValidationForpdfFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.pdf");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForPDFFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.PDF");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationFordocFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.doc");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForDOCFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.DOC");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationFordocxFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.docx");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForDOCXFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.DOCX");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForjpgFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.jpg");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForJPGFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.JPG");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForjpegFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.jpeg");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForJPEGFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.JPEG");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForTxtFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.txt");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForTXTFileNameExtenstion() {
        final boolean valid = documentTypeValidator.isValid("fileName.TXT");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForPdfExtenstionWithSpace() {
        final boolean valid = documentTypeValidator.isValid("fileName .pdf");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForPdfExtenstionWithSpaceNoUnderscore() {
        final boolean valid = documentTypeValidator.isValid("fileName xyz .pdf");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldPassValidationForPdfExtenstionWithUnderscoreAndSpace() {
        final boolean valid = documentTypeValidator.isValid("file_Name_ A_B .pdf");
        assertThat(valid, is(true));
    }

    @Test
    public void shouldFailValidationForNoExtenstion() {
        final boolean valid = documentTypeValidator.isValid("file_Name_ A_Bpdf");
        assertThat(valid, is(false));
    }
}