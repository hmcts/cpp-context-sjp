package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;
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
    protected ReferenceDataService referenceDataService;

    protected UUID caseId = UUID.randomUUID();

    protected UUID defendantId = UUID.randomUUID();

    protected final static ZonedDateTime now = new UtcClock().now();

    protected JsonEnvelope sourceEnvelope = createEnvelope("dummy", createObjectBuilder()
            .add("caseId", caseId.toString())
            .add("created", ZonedDateTimes.toString(now))
            .build());

    void testResultCode() {
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(caseId);
        caseDetail.setUrn(null);
        caseDetail.setEnterpriseId(null);
        caseDetail.setProsecutingAuthority(getProsecutingAuthority());
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
        //when(referenceDataService.getAllFixedList(sourceEnvelope)).thenReturn(Optional.of(readJsonFromFile("data/referencedata.get-all-fixed-list.json")));

        final JsonObject result = givenResult();

        final String requestedCode = result.getString("code");
        final Optional<ResultCode> code = ResultCode.parse(requestedCode);

        assertTrue(code.isPresent(), format("Unknown code: %s", requestedCode));

        final ResultCode resultCode = code.get();

        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "DVLA")
                .add("policeFlag", false)
                .build();

        final CaseView caseView = new CaseView(caseDetail, prosecutorPayload);

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
