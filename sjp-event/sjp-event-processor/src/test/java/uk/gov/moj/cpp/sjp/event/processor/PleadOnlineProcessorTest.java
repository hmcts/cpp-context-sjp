package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;


import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleadOnlineProcessorTest {
    private static final String PLEAD_ONLINE_NAME = "public.prosecutioncasefile.sjp-plead-online";
    private static final String CONTROLLER_PLEAD_ONLINE_COMMAND_NAME = "sjp.command.plead-online";
    private static final String CASE_HAS_BEEN_REVIEWED_EXCEPTION_MESSAGE = "{\"CaseAlreadyReviewed\":[\"Your case has already been reviewed - Contact the Contact Centre if you need to discuss it\"]}";
    private static final String PLEA_ALREADY_SUBMITTED_EXCEPTION_MESSAGE = "{\"PleaAlreadySubmitted\":[\"Plea already submitted - Contact the Contact Centre if you need to change or discuss it\"]}";
    private static final String PLEA_IS_ADJOURNED_POST_CONVENTION_EXCEPTION_MESSAGE = "{\"CaseAdjournedPostConviction\":[\"Your case has already been reviewed - Contact the Contact Centre if you need to discuss it\"]}";

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> queryEnvelopeCaptor;

    @Spy
    @SuppressWarnings("unused")
    private ObjectToJsonValueConverter objectToJsonValueConverter =
            new JsonObjectConvertersFactory().objectToJsonValueConverter();
    @Spy
    @SuppressWarnings("unused")
    private ObjectToJsonObjectConverter objectToJsonObjectConverter =
            new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @InjectMocks
    private PleadOnlineProcessor pleadOnline;

    private final UUID caseId = UUID.randomUUID();

    @Test
    public void shouldPleaNotGuiltyWithLowerCasePostcodeInPersonalDetailsAndInEmployer() {
        final JsonObject pleadOnline = buildPleadOnline(
                "NOT_GUILTY",
                caseId,
                null,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("se11pj")),
                buildEmployerWithAddress(buildAddressObjectWithPostcode(" w1t1jy ")));

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_NAME),
                pleadOnline);

        final JsonEnvelope caseDetailResponseEnvelope = getCaseDetailResponseEnvelope(getValidCaseDetail());

        when(requester.requestAsAdmin(queryEnvelopeCaptor.capture())).thenReturn(caseDetailResponseEnvelope);

        this.pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyNoInteractions(objectToJsonValueConverter);

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));

        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(buildPleadOnline(
                "NOT_GUILTY",
                caseId,
                null,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("SE1 1PJ")),
                buildEmployerWithAddress(buildAddressObjectWithPostcode("W1T 1JY"))))));
    }

    @Test
    public void shouldPleadOnlineNotGuiltyWithoutFinances() {
        final JsonObject pleadOnline = buildPleadOnline(
                "NOT_GUILTY",
                caseId,
                null);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_NAME),
                pleadOnline);


        final JsonEnvelope caseDetailResponseEnvelope = getCaseDetailResponseEnvelope(getValidCaseDetail());

        when(requester.requestAsAdmin(queryEnvelopeCaptor.capture())).thenReturn(caseDetailResponseEnvelope);

        this.pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyNoInteractions(objectToJsonValueConverter);

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));
        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(pleadOnline)));

        verifyNoInteractions(objectToJsonValueConverter);
    }

    @Test
    public void shouldPleadOnlineNotGuiltyAndWithFinances() {
        final JsonObject pleadOnline = buildPleadOnline(
                "NOT_GUILTY",
                caseId,
                createObjectBuilder().build());

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_NAME),
                pleadOnline);


        final JsonEnvelope caseDetailResponseEnvelope = getCaseDetailResponseEnvelope(getValidCaseDetail());

        when(requester.requestAsAdmin(queryEnvelopeCaptor.capture())).thenReturn(caseDetailResponseEnvelope);

        this.pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyNoInteractions(objectToJsonValueConverter);

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));
        assertThat(newCommand.payload(), is(objectToJsonValueConverter.convert(pleadOnline)));
    }

    @Test
    public void shouldPleadOnlineGuiltyAndWithoutFinances() {
        shouldPleadOnlineGuilty(null);
    }

    @Test
    public void shouldPleadOnlineGuiltyAndWithEmptyFinances() {
        shouldPleadOnlineGuilty(createObjectBuilder().build());
    }

    // it should call command, command will raise a rejected event.
    @Test
    public void shouldPleadOnlineWhenAlreadyPleaded() {
        final JsonObject pleadOnline = buildPleadOnline(
                "NOT_GUILTY",
                caseId,
                createObjectBuilder().build());

        JsonObject caseDetail = getCaseDetail("NOT_GUILTY", JsonValue.FALSE);
        invokePleadOnlineAndVerify(pleadOnline, caseDetail);
    }

    @Test
    public void shouldNotPleadOnlineWhenCaseReviewed() {
        final JsonObject pleadOnline = buildPleadOnline(
                "NOT_GUILTY",
                caseId,
                createObjectBuilder().build());
        JsonObject caseDetail = getCaseDetail("GUILTY", JsonValue.TRUE);

        var e = assertThrows(BadRequestException.class, () -> invokePleadOnlineAndVerify(pleadOnline, caseDetail));
        assertThat(e.getMessage(), CoreMatchers.is(CASE_HAS_BEEN_REVIEWED_EXCEPTION_MESSAGE));
    }
    @Test
    public void shouldNotPleadOnlineWhenCasePostAdjourned() {
        final JsonObject pleadOnline = buildPleadOnline(
                "NOT_GUILTY",
                caseId,
                createObjectBuilder().build());

        JsonObject postConventionCaseDetail = getCaseDetailPostConvention("2019-08-11", "FOUND_GUILTY", "2019-08-11");
        var e = assertThrows(BadRequestException.class, () -> invokePleadOnlineAndVerify(pleadOnline, postConventionCaseDetail));
        assertThat(e.getMessage(), CoreMatchers.is(PLEA_IS_ADJOURNED_POST_CONVENTION_EXCEPTION_MESSAGE));
    }

    @Test
    public void shouldPleadOnlineGuiltyAndWithFinances() {
        final JsonObject pleadOnline = getPleadOnlineGuiltyAndWithFinances();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_NAME),
                pleadOnline);


        final JsonEnvelope caseDetailResponseEnvelope = getCaseDetailResponseEnvelope(getValidCaseDetail());

        when(requester.requestAsAdmin(queryEnvelopeCaptor.capture())).thenReturn(caseDetailResponseEnvelope);

        this.pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyNoInteractions(objectToJsonValueConverter);

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));
        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(pleadOnline)));

        verifyNoInteractions(objectToJsonValueConverter);
    }


    private void invokePleadOnlineAndVerify(JsonObject pleadOnline, JsonObject caseDetail) {
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_NAME),
                pleadOnline);

        if(caseDetail != null) {
            final JsonEnvelope caseDetailResponseEnvelope = getCaseDetailResponseEnvelope(caseDetail);
            when(requester.requestAsAdmin(queryEnvelopeCaptor.capture())).thenReturn(caseDetailResponseEnvelope);
        }

        this.pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyNoInteractions(objectToJsonValueConverter);
    }


    private void shouldPleadOnlineGuilty(final JsonObject financialMeans) {
        final JsonObject pleadOnline = buildPleadOnline(
                "GUILTY",
                caseId,
                financialMeans);

        var e = assertThrows(BadRequestException.class, () -> {
            invokePleadOnlineAndVerify(pleadOnline, null);
        });
        assertThat(e.getMessage(), CoreMatchers.is("{\"FinancialMeansRequiredWhenPleadingGuilty\":[\"Financial Means are required when you are pleading GUILTY\"]}"));
    }

    private JsonObject getPleadOnlineGuiltyAndWithFinances() {
        return buildPleadOnline(
                "GUILTY",
                caseId,
                createObjectBuilder()
                        .add("benefits",  createObjectBuilder().add("claimed", true).add("deductPenaltyPreference", true).add("type", "Universal Credit").build())
                        .add("employmentStatus", "EMPLOYED")
                        .add("income", createObjectBuilder().add("amount", BigDecimal.TEN).add("frequency", "FORTNIGHTLY").build())
                        .build());
    }

    private JsonObject getValidCaseDetail() {
        return getCaseDetail(null, JsonValue.FALSE);
    }

    private JsonObject getCaseDetail(final String plea, final JsonValue completed) {
        final JsonObjectBuilder caseDetailBuilder = createObjectBuilder()
                .add("id", caseId.toString())
                .add("completed", completed)
                .add("assigned", JsonValue.FALSE)
                .add("status", NO_PLEA_RECEIVED_READY_FOR_DECISION.name());

        final JsonObjectBuilder offenceObjectBuilder = createObjectBuilder();
        offenceObjectBuilder.add("pendingWithdrawal", JsonValue.FALSE);

        Optional.ofNullable(plea)
                .ifPresent(value -> offenceObjectBuilder.add("plea", plea));

        final JsonArray offences = Json.createArrayBuilder().add(offenceObjectBuilder.build()).build();

        final JsonObject defendant = createObjectBuilder()
                .add("offences", offences)
                .build();

        caseDetailBuilder.add("defendant", defendant);

        return caseDetailBuilder.build();
    }
    private JsonObject getCaseDetailPostConvention( final String adjournedTo, final String convention,final String conventionDate) {
        final JsonObjectBuilder caseDetailBuilder = createObjectBuilder()
                .add("id", caseId.toString())
                .add("adjournedTo", adjournedTo);

        final JsonObjectBuilder offenceObjectBuilder = createObjectBuilder();
        Optional.ofNullable(convention)
                .ifPresent(value ->  offenceObjectBuilder.add("convention", convention));

        Optional.ofNullable(conventionDate)
                .ifPresent(value ->  offenceObjectBuilder.add("conventionDate", conventionDate));


        final JsonArray offences = Json.createArrayBuilder().add(offenceObjectBuilder.build()).build();

        final JsonObject defendant = createObjectBuilder()
                .add("offences", offences)
                .build();

        caseDetailBuilder.add("defendant", defendant);

        return caseDetailBuilder.build();
    }

    private JsonEnvelope getCaseDetailResponseEnvelope(final JsonObject caseDetail) {
        return JsonEnvelope.envelopeFrom(metadataWithRandomUUID("sjp.query.case"), caseDetail);
    }

    public static JsonObject buildPleadOnline(final String plea, final UUID caseId, final JsonObject financialMeans,
                                               final JsonObject personalDetails, final JsonObject employer) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId" , caseId.toString())
                .add("offences" , createArrayBuilder().add(createObjectBuilder()
                                .add("plea" , plea)
                                .build()).build())
                .add("personalDetails", personalDetails)
                .add("employer" , employer);
        if(nonNull(financialMeans)){
            builder.add("financialMeans", financialMeans);
        }
        return builder.build();
    }

    public static JsonObject buildPleadOnline(final String plea, final UUID caseId, final JsonObject financialMeans) {
        final JsonObjectBuilder builder =  createObjectBuilder()
                .add("caseId" , caseId.toString())
                .add("offences" , createArrayBuilder().add(createObjectBuilder()
                        .add("plea" , plea)
                        .build()).build())
                .add("personalDetails",buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("SE1 1PJ")));

        if(nonNull(financialMeans)){
            builder.add("financialMeans", financialMeans);
        }
        return builder.build();
    }

     static JsonObject buildAddressObjectWithPostcode(final String postcode) {
        return createObjectBuilder()
                .add("address1", "14 Tottenham Court Road")
                .add("address3", "London")
                .add("postcode", postcode)
                .build();
    }

     static JsonObject buildPersonalDetailsWithAddress(final JsonObject address) {
        return createObjectBuilder()
                .add("firstName", "John")
                .add("lastName", "Doe")
                .add("dateOfBirth" ,"01/01/1999")
                .add("address", address)
                .build();
    }

    static JsonObject buildEmployerWithAddress(final JsonObject address) {
        return createObjectBuilder()
                .add("employeeReference", "12345")
                .add("name", "Pret a manger")
                .add("phone" , "020 7998 0007")
                .add("address" , address)
                .build();
    }
}
