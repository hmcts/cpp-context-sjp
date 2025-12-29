package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans.financialMeans;
import static uk.gov.justice.json.schemas.domains.sjp.command.Plea.GUILTY;
import static uk.gov.justice.json.schemas.domains.sjp.command.Plea.NOT_GUILTY;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildAddressObjectWithPostcode;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildEmployerWithAddress;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildPersonalDetailsWithAddress;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildPleadOnline;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildPleadAocpOnline;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;

import org.hamcrest.CoreMatchers;
import uk.gov.justice.json.schemas.domains.sjp.command.Benefits;
import uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans;
import uk.gov.justice.json.schemas.domains.sjp.command.Frequency;
import uk.gov.justice.json.schemas.domains.sjp.command.Income;
import uk.gov.justice.json.schemas.domains.sjp.command.Plea;
import uk.gov.justice.json.schemas.domains.sjp.command.PleadAocpOnline;
import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.sjp.command.api.validator.PleadOnlineValidator;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleadOnlineApiTest {

    private static final String PLEAD_ONLINE_COMMAND_NAME = "sjp.plead-online";
    private static final String CONTROLLER_PLEAD_ONLINE_COMMAND_NAME = "sjp.command.plead-online";
    private static final String CONTROLLER_PLEAD_ONLINE_COMMAND_AOCP_NAME = "sjp.command.plead-aocp-online";
    private static final String CASE_HAS_BEEN_REVIEWED_EXCEPTION_MESSAGE = "{\"CaseAlreadyReviewed\":[\"Your case has already been reviewed - Contact the Contact Centre if you need to discuss it\"]}";
    private static final String PLEA_ALREADY_SUBMITTED_EXCEPTION_MESSAGE = "{\"PleaAlreadySubmitted\":[\"Plea already submitted - Contact the Contact Centre if you need to change or discuss it\"]}";
    private static final String PLEA_IS_ADJOURNED_POST_CONVENTION_EXCEPTION_MESSAGE = "{\"CaseAdjournedPostConviction\":[\"Your case has already been reviewed - Contact the Contact Centre if you need to discuss it\"]}";

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<Envelope<PleadOnline>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<PleadAocpOnline>> aocpEnvelopeCaptor;


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

    @Spy
    @SuppressWarnings("unused")
    private PleadOnlineValidator pleadOnlineValidator = new PleadOnlineValidator();

    @InjectMocks
    private PleadOnlineApi pleadOnline;

    private final UUID caseId = UUID.randomUUID();
    private final UUID defendantId = UUID.randomUUID();

    @Test
    public void shouldPleaNotGuiltyWithLowerCasePostcodeInPersonalDetailsAndInEmployer() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                null,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("se11pj")),
                buildEmployerWithAddress(buildAddressObjectWithPostcode(" w1t1jy ")));

        final Envelope envelope = envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_COMMAND_NAME),
                pleadOnline);

        final JsonEnvelope caseDetailResponseEnvelope = getCaseDetailResponseEnvelope(getValidCaseDetail());

        when(requester.requestAsAdmin(queryEnvelopeCaptor.capture())).thenReturn(caseDetailResponseEnvelope);

        this.pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyNoInteractions(objectToJsonValueConverter);

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));

        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(buildPleadOnline(
                NOT_GUILTY,
                caseId,
                null,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("SE1 1PJ")),
                buildEmployerWithAddress(buildAddressObjectWithPostcode("W1T 1JY"))))));
    }

    @Test
    public void shouldPleadOnlineNotGuiltyWithoutFinances() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                null);

        final Envelope<PleadOnline> envelope = envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_COMMAND_NAME),
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
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                financialMeans().build());

        final Envelope<PleadOnline> envelope = envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_COMMAND_NAME),
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
        shouldPleadOnlineGuilty(financialMeans().build());
    }

    @Test
    public void shouldNotPleadOnlineWhenAlreadyPleaded() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                financialMeans().build());

        JsonObject caseDetail = getCaseDetail(NOT_GUILTY, JsonValue.FALSE);
        var e = assertThrows(BadRequestException.class, () -> invokePleadOnlineAndVerify(pleadOnline, caseDetail));
        assertThat(e.getMessage(), CoreMatchers.is(PLEA_ALREADY_SUBMITTED_EXCEPTION_MESSAGE));
    }

    @Test
    public void shouldNotPleadOnlineWhenCaseReviewed() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                financialMeans().build());
        JsonObject caseDetail = getCaseDetail(GUILTY, JsonValue.TRUE);

        var e = assertThrows(BadRequestException.class, () -> invokePleadOnlineAndVerify(pleadOnline, caseDetail));
        assertThat(e.getMessage(), CoreMatchers.is(CASE_HAS_BEEN_REVIEWED_EXCEPTION_MESSAGE));
    }
    @Test
    public void shouldNotPleadOnlineWhenCasePostAdjourned() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                financialMeans().build());

        JsonObject postConventionCaseDetail = getCaseDetailPostConvention("2019-08-11", "FOUND_GUILTY", "2019-08-11");
        var e = assertThrows(BadRequestException.class, () -> invokePleadOnlineAndVerify(pleadOnline, postConventionCaseDetail));
        assertThat(e.getMessage(), CoreMatchers.is(PLEA_IS_ADJOURNED_POST_CONVENTION_EXCEPTION_MESSAGE));
    }

    @Test
    public void shouldPleadOnlineGuiltyAndWithFinances() {
        final PleadOnline pleadOnline = getPleadOnlineGuiltyAndWithFinances();

        final Envelope<PleadOnline> envelope = envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_COMMAND_NAME),
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
    public void shouldHandlePleadAocpOnline() {
        final PleadAocpOnline pleadAocpOnline = buildPleadAocpOnline(
                NOT_GUILTY,
                caseId,
                defendantId,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("se11pj")));

        final Envelope<PleadAocpOnline> envelope = envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_COMMAND_NAME),
                pleadAocpOnline);

        pleadOnline.pleadAocpOnline(envelope);

        verify(sender).send(aocpEnvelopeCaptor.capture());

        final Envelope newCommand = aocpEnvelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_AOCP_NAME));

        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(buildPleadAocpOnline(
                NOT_GUILTY,
                caseId,
                defendantId,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("SE1 1PJ"))))));
    }

    private void invokePleadOnlineAndVerify(PleadOnline pleadOnline, JsonObject caseDetail) {
        final Envelope<PleadOnline> envelope = envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_COMMAND_NAME),
                pleadOnline);

        if(caseDetail != null) {
            final JsonEnvelope caseDetailResponseEnvelope = getCaseDetailResponseEnvelope(caseDetail);
            when(requester.requestAsAdmin(queryEnvelopeCaptor.capture())).thenReturn(caseDetailResponseEnvelope);
        }

        this.pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyNoInteractions(objectToJsonValueConverter);
    }


    private void shouldPleadOnlineGuilty(final FinancialMeans financialMeans) {
        final PleadOnline pleadOnline = buildPleadOnline(
                GUILTY,
                caseId,
                financialMeans);

        var e = assertThrows(BadRequestException.class, () -> {
            invokePleadOnlineAndVerify(pleadOnline, null);
        });
        assertThat(e.getMessage(), CoreMatchers.is("{\"FinancialMeansRequiredWhenPleadingGuilty\":[\"Financial Means are required when you are pleading GUILTY\"]}"));
    }

    private PleadOnline getPleadOnlineGuiltyAndWithFinances() {
        return buildPleadOnline(
                GUILTY,
                caseId,
                financialMeans()
                        .withBenefits(new Benefits(true, true, "Universal Credit"))
                        .withEmploymentStatus("EMPLOYED")
                        .withIncome(new Income(BigDecimal.TEN, Frequency.FORTNIGHTLY))
                        .build());
    }

    private JsonObject getValidCaseDetail() {
        return getCaseDetail(null, JsonValue.FALSE);
    }

    private JsonObject getCaseDetail(final Plea plea, final JsonValue completed) {
        final JsonObjectBuilder caseDetailBuilder = JsonObjects.createObjectBuilder()
                .add("id", caseId.toString())
                .add("completed", completed)
                .add("assigned", JsonValue.FALSE)
                .add("status", NO_PLEA_RECEIVED_READY_FOR_DECISION.name());

        final JsonObjectBuilder offenceObjectBuilder = JsonObjects.createObjectBuilder();
        offenceObjectBuilder.add("pendingWithdrawal", JsonValue.FALSE);

        Optional.ofNullable(plea)
                .ifPresent(value -> offenceObjectBuilder.add("plea", plea.name()));

        final JsonArray offences = JsonObjects.createArrayBuilder().add(offenceObjectBuilder.build()).build();

        final JsonObject defendant = JsonObjects.createObjectBuilder()
                .add("offences", offences)
                .build();

        caseDetailBuilder.add("defendant", defendant);

        return caseDetailBuilder.build();
    }
    private JsonObject getCaseDetailPostConvention( final String adjournedTo, final String convention,final String conventionDate) {
        final JsonObjectBuilder caseDetailBuilder = JsonObjects.createObjectBuilder()
                .add("id", caseId.toString())
                .add("adjournedTo", adjournedTo);

        final JsonObjectBuilder offenceObjectBuilder = JsonObjects.createObjectBuilder();
        Optional.ofNullable(convention)
                .ifPresent(value ->  offenceObjectBuilder.add("convention", convention));

        Optional.ofNullable(conventionDate)
                .ifPresent(value ->  offenceObjectBuilder.add("conventionDate", conventionDate));


        final JsonArray offences = JsonObjects.createArrayBuilder().add(offenceObjectBuilder.build()).build();

        final JsonObject defendant = JsonObjects.createObjectBuilder()
                .add("offences", offences)
                .build();

        caseDetailBuilder.add("defendant", defendant);

        return caseDetailBuilder.build();
    }

    private JsonEnvelope getCaseDetailResponseEnvelope(final JsonObject caseDetail) {
        return envelopeFrom(metadataWithRandomUUID("sjp.query.case"), caseDetail);
    }
}
