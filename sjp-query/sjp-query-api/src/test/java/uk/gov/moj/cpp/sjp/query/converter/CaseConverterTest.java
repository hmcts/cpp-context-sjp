package uk.gov.moj.cpp.sjp.query.converter;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.sjp.query.api.converter.CaseConverter;
import uk.gov.moj.cpp.sjp.query.api.service.ReferenceOffencesDataService;

@RunWith(MockitoJUnitRunner.class)
public class CaseConverterTest {

    @InjectMocks
    private CaseConverter caseConverter;

    @Mock
    private ReferenceOffencesDataService referenceOffencesDataService;

    @Test
    public void shouldAddPersonInfoForDefendantWithMatchingPostcode() {
        final UUID caseId = UUID.randomUUID();
        final String urn = "TFL123456";
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final boolean completed = false;
        final boolean assigned = true;
        final boolean policeFlag = false;
        final boolean postConviction = false;
        final String status = "NO_PLEA_RECEIVED";

        final String firstName = "John";
        final String lastName = "Faceless";
        final LocalDate dob = LocalDate.now();

        final String address1 = "Street";
        final String address2 = "Landsowne";
        final String address3 = "Oxford";
        final String address4 = "Surrey";
        final String address5 = "Greater London";
        final String postcode = "OX4GF1";

        final String offenceCode = "OF611";
        final String offenceStartDate = "2010-01-01";
        final String plea = "GUILTY";
        final boolean pendingWithdrawal = true;

        final String title = "this is offence title";
        final String wording = "this is offence wording";
        final String wordingWelsh = "this is offence wording in Welsh";
        final String legislation = "legislation";

        // reference data
        final String titleWelsh = "this is the Welsh offence title";
        final String legislationWelsh = "this is the Welsh legislation";

        final JsonObject caseDetails = createObjectBuilder()
                .add("id", caseId.toString())
                .add("urn", urn)
                .add("completed", completed)
                .add("assigned", assigned)
                .add("aocpEligible", false)
                .add("readyForDecision", false)
                .add("policeFlag", policeFlag)
                .add("postConviction", postConviction)
                .add("status", status)
                .add("costs", 1.0)
                .add("aocpVictimSurcharge", 1.0)
                .add("aocpTotalCost", 5.0)
                .add("resultedThroughAocp", true)
                .add("defendant", createObjectBuilder()
                        .add("id", defendantId.toString())
                        .add("personalDetails", createObjectBuilder()
                                .add("firstName", firstName)
                                .add("lastName", lastName)
                                .add("dateOfBirth", LocalDates.to(dob))
                                .add("address", createObjectBuilder()
                                        .add("address1", address1)
                                        .add("address2", address2)
                                        .add("address3", address3)
                                        .add("address4", address4)
                                        .add("address5", address5)
                                        .add("postcode", postcode)
                                )
                        )
                        .add("offences", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", offenceId.toString())
                                        .add("offenceCode", offenceCode)
                                        .add("startDate", offenceStartDate)
                                        .add("plea", plea)
                                        .add("pendingWithdrawal", pendingWithdrawal)
                                        .add("wording", wording)
                                        .add("wordingWelsh", wordingWelsh)
                                        .add("endorsable", true)
                                        .add("compensation", 1.0)
                                        .add("aocpStandardPenalty", 2.0)
                                )
                        )
                ).build();

        final JsonEnvelope request = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-by-urn-postcode")).build();

        final JsonObject offenceReferenceData = buildOffenceReferenceData(title, legislation, titleWelsh, legislationWelsh);

        when(referenceOffencesDataService.getOffenceReferenceData(request, offenceCode, offenceStartDate)).thenReturn(offenceReferenceData);

        final JsonObject result = caseConverter.addOffenceReferenceDataToOffences(caseDetails, request);

        final JsonObject expectedResult = createObjectBuilder()
                .add("id", caseId.toString())
                .add("urn", urn)
                .add("completed", completed)
                .add("assigned", assigned)
                .add("aocpEligible", false)
                .add("readyForDecision", false)
                .add("policeFlag", policeFlag)
                .add("postConviction", postConviction)
                .add("status", status)
                .add("costs", 1.0)
                .add("aocpVictimSurcharge", 1.0)
                .add("aocpTotalCost", 5.0)
                .add("resultedThroughAocp", true)
                .add("defendant", createObjectBuilder()
                        .add("id", defendantId.toString())
                        .add("personalDetails", createObjectBuilder()
                                .add("firstName", firstName)
                                .add("lastName", lastName)
                                .add("dateOfBirth", LocalDates.to(dob))
                                .add("address", createObjectBuilder()
                                        .add("address1", address1)
                                        .add("address2", address2)
                                        .add("address3", address3)
                                        .add("address4", address4)
                                        .add("address5", address5)
                                        .add("postcode", postcode)
                                )
                        )
                        .add("offences", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", offenceId.toString())
                                        .add("wording", wording)
                                        .add("pendingWithdrawal", pendingWithdrawal)
                                        .add("title", title)
                                        .add("legislation", legislation)
                                        .add("wordingWelsh", wordingWelsh)
                                        .add("titleWelsh", titleWelsh)
                                        .add("legislationWelsh", legislationWelsh)
                                        .add("plea", plea)
                                        .add("endorsable", true)
                                        .add("compensation", 1.0)
                                        .add("aocpStandardPenalty", 2.0)
                                )
                        )
                ).build();

        assertThat(result, is(expectedResult));
    }

    private JsonObject buildOffenceReferenceData(final String title, final String legislation,
                                                 final String titleWelsh, final String legislationWelsh) {
        return Json.createObjectBuilder()
                .add("title", title)
                .add("legislation", legislation)
                .add("details", Json.createObjectBuilder()
                        .add("document", Json.createObjectBuilder()
                                .add("welsh", Json.createObjectBuilder()
                                        .add("welshoffencetitle", titleWelsh)
                                        .add("welshlegislation", legislationWelsh)
                                )))
                .build();
    }
}