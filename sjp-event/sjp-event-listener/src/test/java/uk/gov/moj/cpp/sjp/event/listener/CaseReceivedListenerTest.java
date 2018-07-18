package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Collections.singleton;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.event.listener.converter.CaseReceivedToCase;
import uk.gov.moj.cpp.sjp.event.listener.converter.ContactDetailsToContactDetailsEntity;
import uk.gov.moj.cpp.sjp.event.listener.converter.DefendantToDefendantDetails;
import uk.gov.moj.cpp.sjp.event.listener.converter.OffenceToOffenceDetail;
import uk.gov.moj.cpp.sjp.event.listener.converter.PersonToPersonalDetailsEntity;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReceivedListenerTest {

    @InjectMocks
    private CaseCreatedListener listener;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseSearchResultRepository searchResultRepository;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AddressToAddressEntity addressToAddressEntityConverter;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntityConverter;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private OffenceToOffenceDetail offenceToOffenceDetailConverter;

    @Spy
    @InjectMocks
    private PersonToPersonalDetailsEntity personToPersonalDetailsEntity = new PersonToPersonalDetailsEntity();

    @Spy
    @InjectMocks
    private DefendantToDefendantDetails defendantToDefendantDetailsConverter = new DefendantToDefendantDetails();

    @Spy
    @InjectMocks
    private CaseReceivedToCase caseReceivedToCaseConverter = new CaseReceivedToCase();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private CaseSearchResultService caseSearchResultService = new CaseSearchResultService();

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter converter = new JsonObjectToObjectConverter();

    private static final UUID caseId = UUID.randomUUID();
    private static final ProsecutingAuthority prosecutingAuthority = TFL;
    private static final String urn = prosecutingAuthority.name() + "1234";
    private static final String enterpriseId = RandomStringUtils.randomAlphanumeric(12).toUpperCase();
    private static final BigDecimal costs = BigDecimal.valueOf(12.23);
    private static final LocalDate postingDate = LocalDate.of(2017, 1, 1);
    private static final UUID defendantId = UUID.randomUUID();

    private static final String defendantTitle = "Mr";
    private static final String defendantFirstName = "John";
    private static final String defendantLastName = "Smith";
    private static final LocalDate defendantDateOfBirth = LocalDate.of(1960, 1, 1);
    private static final String defendantGender = "Male";
    private static final int numPreviousConvictions = 2;

    private static final String address1 = "Flat 1, Apple Building";
    private static final String address2 = "6 Luxury Street";
    private static final String address3 = "Croydon";
    private static final String address4 = "London";
    private static final String postcode = "CR16BY";
    private static final String caseCreatedOn = "2018-01-11T16:17:15.294Z[UTC]";
    private static final UUID offenceId = UUID.randomUUID();
    private static final int offenceSequenceNo = 1;
    private static final String offenceCode = "PS00001";
    private static final LocalDate chargeDate = LocalDate.of(2017, 1, 1);
    private static final LocalDate offenceDate = LocalDate.of(2017, 1, 5);
    private static final String offenceWording = "this is offence wording";
    private static final String prosecutionFacts = "this is prosecution facts";
    private static final String witnessStatement = "this is witness statement";
    private static final BigDecimal compensation = BigDecimal.valueOf(2.34);

    @Test
    public void shouldSaveTheCaseAndSearchResult() {
        JsonEnvelope envelope = givenCaseReceivedEventWasRaised();

        whenItIsPickedUpByListener(envelope);

        itStoresDetailsIntoCaseAndCaseSearchRepository();
    }

    private void itStoresDetailsIntoCaseAndCaseSearchRepository() {
        verifyCaseSaved();
        verifyCaseSearchSaved();
    }

    private void verifyCaseSearchSaved() {
        ArgumentCaptor<CaseSearchResult> resultCaptor = ArgumentCaptor.forClass(CaseSearchResult.class);
        verify(searchResultRepository).save(resultCaptor.capture());

        final CaseSearchResult result = resultCaptor.getValue();
        assertThat(result.getCaseId(), is(caseId));
        assertThat(result.getFirstName(), is(defendantFirstName));
        assertThat(result.getLastName(), is(defendantLastName));
        assertThat(result.getCurrentFirstName(), is(defendantFirstName));
        assertThat(result.getCurrentLastName(), is(defendantLastName));
        assertThat(result.getDateOfBirth(), is(defendantDateOfBirth));
    }

    private void verifyCaseSaved() {
        final ArgumentCaptor<CaseDetail> caseDetailCaptor = ArgumentCaptor.forClass(CaseDetail.class);
        verify(caseRepository).save(caseDetailCaptor.capture());

        final CaseDetail actualCaseDetail = caseDetailCaptor.getValue();
        final CaseDetail expectedCaseDetail = new CaseDetail(
                caseId,
                urn,
                enterpriseId,
                prosecutingAuthority,
                null,
                false,
                null,
                ZonedDateTimes.fromString(caseCreatedOn),
                new DefendantDetail(
                        defendantId,
                        new PersonalDetails(
                                defendantTitle,
                                defendantFirstName,
                                defendantLastName,
                                defendantDateOfBirth,
                                defendantGender,
                                null,
                                new Address(
                                        address1,
                                        address2,
                                        address3,
                                        address4,
                                        postcode
                                ),
                                null
                        ),
                        singleton(
                                new OffenceDetail.OffenceDetailBuilder()
                                        .setId(offenceId)
                                        .setChargeDate(chargeDate)
                                        .setCode(offenceCode)
                                        .setSequenceNumber(offenceSequenceNo)
                                        .setStartDate(offenceDate)
                                        .setWording(offenceWording)
                                        .withCompensation(compensation)
                                        .withLibraOffenceDateCode(1)
                                        .withProsecutionFacts(prosecutionFacts)
                                        .withWitnessStatement(witnessStatement)
                                        .build()
                        ),
                        numPreviousConvictions
                ),
                costs,
                postingDate);

        final DefendantDetail actualDefendant = actualCaseDetail.getDefendant();
        final DefendantDetail expectedDefendant = expectedCaseDetail.getDefendant();

        final PersonalDetails actualPersonalDetails = actualDefendant.getPersonalDetails();
        final PersonalDetails expectedPersonalDetails = expectedDefendant.getPersonalDetails();

        assertThat(actualPersonalDetails.getAddress().getAddress1(),
                equalTo(expectedPersonalDetails.getAddress().getAddress1()));
        assertThat(actualPersonalDetails.getAddress().getAddress2(),
                equalTo(expectedPersonalDetails.getAddress().getAddress2()));
        assertThat(actualPersonalDetails.getAddress().getAddress3(),
                equalTo(expectedPersonalDetails.getAddress().getAddress3()));
        assertThat(actualPersonalDetails.getAddress().getAddress4(),
                equalTo(expectedPersonalDetails.getAddress().getAddress4()));
        assertThat(actualPersonalDetails.getAddress().getPostcode(),
                equalTo(expectedPersonalDetails.getAddress().getPostcode()));

        assertThat(actualPersonalDetails.getFirstName(), equalTo(expectedPersonalDetails.getFirstName()));
        assertThat(actualPersonalDetails.getLastName(), equalTo(expectedPersonalDetails.getLastName()));
        assertThat(actualPersonalDetails.getDateOfBirth(), equalTo(expectedPersonalDetails.getDateOfBirth()));
        assertThat(actualPersonalDetails.getGender(), equalTo(expectedPersonalDetails.getGender()));
        assertThat(actualPersonalDetails.getTitle(), equalTo(expectedPersonalDetails.getTitle()));

        assertThat(actualDefendant.getNumPreviousConvictions(), equalTo(expectedDefendant.getNumPreviousConvictions()));
        assertThat(actualDefendant.getId(), equalTo(expectedDefendant.getId()));

        assertThat(actualDefendant.getOffences().size(), equalTo(1));
        final OffenceDetail actualOffenceDetail = actualDefendant.getOffences().iterator().next();
        final OffenceDetail expectedOffenceDetail = expectedDefendant.getOffences().iterator().next();
        assertThat(actualOffenceDetail.getChargeDate(), equalTo(expectedOffenceDetail.getChargeDate()));
        assertThat(actualOffenceDetail.getId(), equalTo(expectedOffenceDetail.getId()));
        assertThat(actualOffenceDetail.getCode(), equalTo(expectedOffenceDetail.getCode()));
        assertThat(actualOffenceDetail.getCompensation(), equalTo(expectedOffenceDetail.getCompensation()));
        assertThat(actualOffenceDetail.getLibraOffenceDateCode(), equalTo(expectedOffenceDetail.getLibraOffenceDateCode()));
        assertThat(actualOffenceDetail.getProsecutionFacts(), equalTo(expectedOffenceDetail.getProsecutionFacts()));
        assertThat(actualOffenceDetail.getWording(), equalTo(expectedOffenceDetail.getWording()));
        assertThat(actualOffenceDetail.getSequenceNumber(), equalTo(expectedOffenceDetail.getSequenceNumber()));
        assertThat(actualOffenceDetail.getStartDate(), equalTo(expectedOffenceDetail.getStartDate()));
        assertThat(actualOffenceDetail.getWitnessStatement(), equalTo(expectedOffenceDetail.getWitnessStatement()));

        assertThat(actualCaseDetail.getCosts(), equalTo(expectedCaseDetail.getCosts()));
        assertThat(actualCaseDetail.getPostingDate(), equalTo(expectedCaseDetail.getPostingDate()));
        assertThat(actualCaseDetail.getId(), equalTo(expectedCaseDetail.getId()));
        assertThat(actualCaseDetail.getUrn(), equalTo(expectedCaseDetail.getUrn()));
        assertThat(actualCaseDetail.getProsecutingAuthority(), equalTo(expectedCaseDetail.getProsecutingAuthority()));
        assertThat(actualCaseDetail.getDateTimeCreated().toEpochSecond(), equalTo(expectedCaseDetail.getDateTimeCreated().toEpochSecond()));

        assertThat(actualCaseDetail.getId(), equalTo(expectedCaseDetail.getId()));
    }

    private void whenItIsPickedUpByListener(final JsonEnvelope envelope) {
        listener.caseReceived(envelope);
    }

    private JsonEnvelope givenCaseReceivedEventWasRaised() {
        final JsonObject caseReceivedEventPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("urn", urn)
                .add("prosecutingAuthority", prosecutingAuthority.name())
                .add("costs", costs)
                .add("postingDate", LocalDates.to(postingDate))
                .add("createdOn", caseCreatedOn)
                .add("defendant", createObjectBuilder()
                        .add("id", defendantId.toString())
                        .add("title", defendantTitle)
                        .add("firstName", defendantFirstName)
                        .add("lastName", defendantLastName)
                        .add("dateOfBirth", LocalDates.to(defendantDateOfBirth))
                        .add("gender", defendantGender)
                        .add("numPreviousConvictions", numPreviousConvictions)
                        .add("address", createObjectBuilder()
                                .add("address1", address1)
                                .add("address2", address2)
                                .add("address3", address3)
                                .add("address4", address4)
                                .add("postcode", postcode)
                        )
                        .add("offences", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", offenceId.toString())
                                        .add("offenceSequenceNo", offenceSequenceNo)
                                        .add("libraOffenceCode", offenceCode)
                                        .add("chargeDate", LocalDates.to(chargeDate))
                                        .add("offenceDate", LocalDates.to(offenceDate))
                                        .add("libraOffenceDateCode", 1)
                                        .add("offenceWording", offenceWording)
                                        .add("prosecutionFacts", prosecutionFacts)
                                        .add("witnessStatement", witnessStatement)
                                        .add("compensation", compensation)
                                )
                        )
                )
                .build();

        return EnvelopeFactory.createEnvelope(CaseReceived.EVENT_NAME, caseReceivedEventPayload);
    }

}
