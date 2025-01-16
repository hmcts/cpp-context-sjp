package uk.gov.moj.sjp.it.helper;

import static org.apache.commons.io.IOUtils.toByteArray;

import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.PdfContentHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

public class PressTransparencyReportHelper {

    public void requestToGeneratePressTransparencyReport(JsonObject payload) {
        final String resource = "/press-transparency-report/request";
        final String contentType = "application/vnd.sjp.request-press-transparency-report+json";
        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    public String requestToGetTransparencyReportPressContent(String contentId) throws IOException {
        final String acceptHeader = "application/vnd.sjp.query.press-transparency-report-content+json";
        final String resource = String.format("/press-transparency-report/content/%s", contentId);

        final Response response = HttpClientUtil.makeGetCall(resource, acceptHeader);

        byte[] resultOrderContent;
        InputStream inputStream = null;
        try {
            inputStream = response.readEntity(InputStream.class);
            resultOrderContent = toByteArray(inputStream);
        } finally {
            inputStream.close();
        }

        final PdfContentHelper pdfHelper = new PdfContentHelper(resultOrderContent);

        return pdfHelper.getTextContent();
    }


    public String getStubbedContent() {
        String payload;
        try {
            payload = IOUtils.toString(Objects.requireNonNull(PressTransparencyReportHelper.class
                    .getClassLoader()
                    .getResourceAsStream("TransparencyReportIT/report-content.txt")));
        } catch (final IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
        return payload;
    }

}
