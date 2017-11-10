package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.helper.CaseSjpHelper.GET_SJP_CASE_BY_URN_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseByUrn;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getSjpCaseByUrn;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class GetCaseHelper extends AbstractTestHelper {

    public void verifyGetCaseByUrn(String urn) {
        poll(getCaseByUrn(urn)).until(
                status().is(Status.OK)
        );
    }

    public void verifyGetSjpCaseByUrn(String urn) {
        poll(getSjpCaseByUrn(urn)).until(
                status().is(Status.OK)
        );

    }

    public void verifyGetSjpCaseByUrnCannotBeFind(String urn) {
        Response response = makeGetCall(getReadUrl("/cases?urn=" + urn), GET_SJP_CASE_BY_URN_MEDIA_TYPE);
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
    }
}
