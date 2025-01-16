package uk.gov.moj.sjp.it.helper;

import static org.apache.commons.io.IOUtils.toByteArray;

import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.PdfContentHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

public class TransparencyReportHelper {

    public void requestToGenerateTransparencyReport(JsonObject payload) {
        final String resource = "/transparency-report/request";
        final String contentType = "application/vnd.sjp.request-transparency-report+json";
        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    public String requestToGetTransparencyReportContent(String contentId) throws IOException {
        final String acceptHeader = "application/vnd.sjp.query.transparency-report-content+json";
        final String resource = String.format("/transparency-report/content/%s", contentId);

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
            payload = IOUtils.toString(TransparencyReportHelper.class
                    .getClassLoader()
                    .getResourceAsStream("TransparencyReportIT/report-content.txt"));
        } catch (final IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
        return payload;
    }

}
