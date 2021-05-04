package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;

import uk.gov.justice.json.schemas.domains.sjp.command.Benefits;
import uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans;
import uk.gov.justice.json.schemas.domains.sjp.command.Frequency;
import uk.gov.justice.json.schemas.domains.sjp.command.Income;
import uk.gov.justice.json.schemas.domains.sjp.command.Plea;
import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.api.validator.PleadOnlineValidator;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleadOnlineApiTest {

    private static final String PLEAD_ONLINE_COMMAND_NAME = "sjp.plead-online";
    private static final String CONTROLLER_PLEAD_ONLINE_COMMAND_NAME = "sjp.command.plead-online";
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
    private ArgumentCaptor<JsonEnvelope> queryEnvelopeCaptor;

    @Spy
    @SuppressWarnings("unused")
    private ObjectToJsonValueConverter objectToJsonValueConverter =
            new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    @SuppressWarnings("unused")
    private PleadOnlineValidator pleadOnlineValidator = new PleadOnlineValidator();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @InjectMocks
    private PleadOnlineApi pleadOnline;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final UUID caseId = UUID.randomUUID();

    @Test
    public void shouldPleaNotGuiltyWithLowerCasePostcodeInPersonalDetailsAndInEmployer() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                null,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("se11pj")),
                buildEmployerWithAddress(buildAddressObjectWithPostcode(" w1t1jy ")));

        pleadOnlineWith(pleadOnline, getValidCaseDetail());

        final Envelope<PleadOnline> newCommand = envelopeCaptor.getValue();
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

        pleadOnlineWith(pleadOnline, getValidCaseDetail());

        final Envelope<PleadOnline> newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));
        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(pleadOnline)));

        verifyZeroInteractions(objectToJsonValueConverter);
    }

    @Test
    public void shouldPleadOnlineNotGuiltyAndWithFinances() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                financialMeans().build());

        pleadOnlineWith(pleadOnline, getValidCaseDetail());

        final Envelope<PleadOnline> newCommand = envelopeCaptor.getValue();
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

        exception.expect(BadRequestException.class);
        exception.expectMessage(PLEA_ALREADY_SUBMITTED_EXCEPTION_MESSAGE);

        pleadOnlineWith(pleadOnline, getCaseDetail(NOT_GUILTY, JsonValue.FALSE));
    }

    @Test
    public void shouldNotPleadOnlineWhenCaseReviewed() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                financialMeans().build());

        exception.expect(BadRequestException.class);
        exception.expectMessage(CASE_HAS_BEEN_REVIEWED_EXCEPTION_MESSAGE);

        pleadOnlineWith(pleadOnline, getCaseDetail(GUILTY, JsonValue.TRUE));
    }
    @Test
    public void shouldNotPleadOnlineWhenCasePostAdjourned() {
        final PleadOnline pleadOnline = buildPleadOnline(
                NOT_GUILTY,
                caseId,
                financialMeans().build());

        exception.expect(BadRequestException.class);
        exception.expectMessage(PLEA_IS_ADJOURNED_POST_CONVENTION_EXCEPTION_MESSAGE);

        pleadOnlineWith(pleadOnline, getCaseDetailPostConvention(  "2019-08-11","FOUND_GUILTY", "2019-08-11"));
    }


    private void shouldPleadOnlineGuilty(final FinancialMeans financialMeans) {
        final PleadOnline pleadOnline = buildPleadOnline(
                GUILTY,
                caseId,
                financialMeans);

        exception.expect(BadRequestException.class);
        exception.expectMessage("{\"FinancialMeansRequiredWhenPleadingGuilty\":[\"Financial Means are required when you are pleading GUILTY\"]}");

        pleadOnlineWith(pleadOnline, getValidCaseDetail());
    }

    @Test
    public void shouldPleadOnlineGuiltyAndWithFinances() {
        final PleadOnline pleadOnline = getPleadOnlineGuiltyAndWithFinances();

        pleadOnlineWith(pleadOnline, getValidCaseDetail());

        final Envelope<PleadOnline> newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));
        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(pleadOnline)));

        verifyZeroInteractions(objectToJsonValueConverter);
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

    private void pleadOnlineWith(final PleadOnline envelopePayload, final JsonObject caseDetail) {
        final Envelope<PleadOnline> envelope = envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_COMMAND_NAME),
                envelopePayload);

        final JsonEnvelope caseDetailResponseEnvelope = getCaseDetailResponseEnvelope(caseDetail);

        when(requester.requestAsAdmin(queryEnvelopeCaptor.capture())).thenReturn(caseDetailResponseEnvelope);

        pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyZeroInteractions(objectToJsonValueConverter);
    }

    private JsonObject getValidCaseDetail() {
        return getCaseDetail(null, JsonValue.FALSE);
    }

    private JsonObject getCaseDetail(final Plea plea, final JsonValue completed) {
        final JsonObjectBuilder caseDetailBuilder = Json.createObjectBuilder()
                .add("id", caseId.toString())
                .add("completed", completed)
                .add("assigned", JsonValue.FALSE)
                .add("status", NO_PLEA_RECEIVED_READY_FOR_DECISION.name());

        final JsonObjectBuilder offenceObjectBuilder = Json.createObjectBuilder();
        offenceObjectBuilder.add("pendingWithdrawal", JsonValue.FALSE);

        Optional.ofNullable(plea)
                .ifPresent(value -> offenceObjectBuilder.add("plea", plea.name()));

        final JsonArray offences = Json.createArrayBuilder().add(offenceObjectBuilder.build()).build();

        final JsonObject defendant = Json.createObjectBuilder()
                .add("offences", offences)
                .build();

        caseDetailBuilder.add("defendant", defendant);

        return caseDetailBuilder.build();
    }
    private JsonObject getCaseDetailPostConvention( final String adjournedTo, final String convention,final String conventionDate) {
        final JsonObjectBuilder caseDetailBuilder = Json.createObjectBuilder()
                .add("id", caseId.toString())
                .add("adjournedTo", adjournedTo);

        final JsonObjectBuilder offenceObjectBuilder = Json.createObjectBuilder();
        Optional.ofNullable(convention)
                .ifPresent(value ->  offenceObjectBuilder.add("convention", convention));

        Optional.ofNullable(conventionDate)
                .ifPresent(value ->  offenceObjectBuilder.add("conventionDate", conventionDate));


        final JsonArray offences = Json.createArrayBuilder().add(offenceObjectBuilder.build()).build();

        final JsonObject defendant = Json.createObjectBuilder()
                .add("offences", offences)
                .build();

        caseDetailBuilder.add("defendant", defendant);

        return caseDetailBuilder.build();
    }

    private JsonEnvelope getCaseDetailResponseEnvelope(final JsonObject caseDetail) {
        return envelopeFrom(metadataWithRandomUUID("sjp.query.case"), caseDetail);
    }
}
