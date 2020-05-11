package uk.gov.moj.sjp.it.helper;

import static org.apache.commons.io.IOUtils.toByteArray;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.PdfContentHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;

public class PressTransparencyReportHelper {

    public void requestToGeneratePressTransparencyReport() {
        final String resource = "/press-transparency-report/request";
        final String contentType = "application/vnd.sjp.request-press-transparency-report+json";
        HttpClientUtil.makePostCall(resource, contentType, "{}");
    }

    public JsonObject pollForPressTransparencyReportMetadata(final Matcher matcher) {
        final String acceptHeader = "application/vnd.sjp.query.press-transparency-report-metadata+json";
        final String resource = "/press-transparency-report/metadata";

        final RequestParamsBuilder requestParams = requestParams(getReadUrl(resource), acceptHeader)
                .withHeader(HeaderConstants.USER_ID, USER_ID);

        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);

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
            payload = IOUtils.toString(PressTransparencyReportHelper.class
                    .getClassLoader()
                    .getResourceAsStream("TransparencyReportIT/report-content.txt"));
        } catch (final IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
        return payload;
    }

}
