package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.moj.cpp.sjp.query.view.util.JsonHelper.readJsonFromFile;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.query.view.converter.ResultCode;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.mockito.Mock;

public abstract class ResultCodeConverterTest {

    @Mock
    private ReferenceDataService referenceDataService;

    private UUID caseId = UUID.randomUUID();

    private UUID defendantId = UUID.randomUUID();

    protected final static ZonedDateTime now = new UtcClock().now();

    private JsonEnvelope sourceEnvelope = createEnvelope("dummy", createObjectBuilder()
            .add("caseId", caseId.toString())
            .add("created", ZonedDateTimes.toString(now))
            .build());

    void testResultCode() {
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(caseId);
        caseDetail.setUrn(null);
        caseDetail.setEnterpriseId(null);
        caseDetail.setProsecutingAuthority(ProsecutingAuthority.valueOf(getProsecutingAuthority()));
        DefendantDetail defendantDetail = new DefendantDetail();
        defendantDetail.setId(defendantId);
        caseDetail.setDefendant(defendantDetail);

        /*when(sjpService.getCaseView(caseId, sourceEnvelope))
                .thenReturn(Optional.of(
                        new CaseView(
                                caseId,
                                null,
                                null,
                                getProsecutingAuthority(),
                                new DefendantView(defendantId, null, null, null, null, null),
                                null)
                        )
                );*/

        /*when(sjpService.getDefendantEmployer(defendantId, sourceEnvelope))
                .thenReturn(createEnvelope("sjp.query.employer", readJsonFromFile("data/sjp.query.employer.json")));
*/
        when(referenceDataService.getAllFixedList(sourceEnvelope)).thenReturn(Optional.of(readJsonFromFile("data/referencedata.get-all-fixed-list.json")));

        final JsonObject result = givenResult();

        final String requestedCode = result.getString("code");
        final Optional<ResultCode> code = ResultCode.parse(requestedCode);

        assertTrue(format("Unknown code: %s", requestedCode), code.isPresent());

        final ResultCode resultCode = code.get();

        final CaseView caseView = new CaseView(caseDetail, "DVLA");

        Employer employer = new Employer(defendantId, "McDonald's", "12345", "020 7998 9300",
                new Address("14 Tottenham Court Road", "London", "England", "UK", "Greater London", "W1T 1JY"));

        final JsonArrayBuilder actualPrompts = resultCode.createPrompts(result,
                OffenceDataSupplier.create(sourceEnvelope, caseView, employer, referenceDataService));

        assertThat(actualPrompts.build(), equalTo(getExpectedPrompts()));
    }

    protected String getProsecutingAuthority() {
        return "DVLA";
    }

    protected abstract JsonObject givenResult();

    protected abstract JsonArray getExpectedPrompts();
}
