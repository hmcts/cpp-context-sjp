package uk.gov.moj.cpp.sjp.query.view;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.query.view.response.ResultOrdersView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResultOrdersTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseService caseService;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    private Clock clock = new StoppedClock(ZonedDateTime.now(ZoneOffset.UTC));

    @Test
    public void getResultOrders() {
        final String FROM_DATE = "2017-01-01";
        final String TO_DATE = "2017-01-10";
        JsonEnvelope request = envelopeFrom(
                metadataWithRandomUUID("sjp.query.result-orders"),
                createObjectBuilder()
                        .add("fromDate", FROM_DATE)
                        .add("toDate", TO_DATE)
                        .build());

        final UUID CASE_ID = UUID.randomUUID();
        final String URN = "urn";
        final UUID DOCUMENT_ID = UUID.randomUUID();
        final ZonedDateTime ADDED_AT = clock.now();
        final ResultOrdersView resultOrdersView = new ResultOrdersView();
        final DefendantDetail defendantDetail=new DefendantDetail();
        final Address address=new Address();
        address.setAddress1("address1");
        address.setAddress2("address2");
        address.setAddress3("address3");
        address.setPostcode("postcode");
        defendantDetail.setAddress(address);

        resultOrdersView.addResultOrder(
                ResultOrdersView.createResultOrderBuilder().setCaseId(CASE_ID).setUrn(URN)
                        .setDefendant(defendantDetail).setOrder(DOCUMENT_ID, ADDED_AT)
                        .build());
        when(caseService.findResultOrders(LocalDates.from(FROM_DATE), LocalDates.from(TO_DATE)))
                .thenReturn(resultOrdersView);

        //when
        final JsonEnvelope response = sjpQueryView.getResultOrders(request);

        //then
        verify(caseService).findResultOrders(LocalDates.from(FROM_DATE), LocalDates.from(TO_DATE));

        assertThat(response, JsonEnvelopeMatcher.jsonEnvelope(
                JsonEnvelopeMetadataMatcher.metadata()
                        .withName("sjp.query.result-orders"),
                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                        JsonPathMatchers.withJsonPath("$.resultOrders[0].caseId",
                                is(CASE_ID.toString())),
                        JsonPathMatchers.withJsonPath("$.resultOrders[0].urn",
                                is(URN)),
                        JsonPathMatchers.withJsonPath("$.resultOrders[0].order.documentId",
                                is(DOCUMENT_ID.toString())),
                        JsonPathMatchers.withJsonPath("$.resultOrders[0].order.addedAt",
                                startsWith(ADDED_AT.toString().substring(0, ADDED_AT.toString().indexOf("."))))))));
    }
}
