package uk.gov.moj.sjp.it.stub;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static java.lang.String.format;

/**
 * Class to set up stub.
 */
public class StubUtil {

    protected static final String DEFAULT_JSON_CONTENT_TYPE = "application/json";

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String PORT = "8080";

    static {
        configureFor(HOST, Integer.parseInt(PORT));
        reset();
    }

    protected static String withQueryParam(String url, String paramName, String paramValue) {
        return format("%s?%s=%s", url, paramName, paramValue);
    }

    protected static String withMultipleQueryParams(String url, Map<String, String> queryParams) {
        StringBuilder urlBuilder = new StringBuilder(url).append("?");
        queryParams.forEach((key, value) -> {
            urlBuilder.append(key + "=" + value + "&");
        });
        String finalUrl = urlBuilder.toString();
        return finalUrl.substring(0, finalUrl.length() - 1);
    }
}
