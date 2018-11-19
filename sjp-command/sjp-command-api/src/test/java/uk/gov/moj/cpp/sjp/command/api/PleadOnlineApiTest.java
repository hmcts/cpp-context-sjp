package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildAddressObjectWithPostcode;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildEmployerWithAddress;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildPersonalDetailsWithAddress;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildPleadOnline;

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
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.command.api.validator.PleadOnlineValidator;

import java.math.BigDecimal;

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

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<PleadOnline>> envelopeCaptor;

    @Spy
    @SuppressWarnings("unused")
    private ObjectToJsonValueConverter objectToJsonValueConverter =
            new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    @InjectMocks
    @SuppressWarnings("unused")
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    @SuppressWarnings("unused")
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    @SuppressWarnings("unused")
    private PleadOnlineValidator pleadOnlineValidator = new PleadOnlineValidator();

    @InjectMocks
    private PleadOnlineApi pleadOnline;

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Test
    public void shouldPleaNotGuiltyWithLowerCasePostcodeInPersonalDetailsAndInEmployer() {
        final PleadOnline pleadOnline = buildPleadOnline(
                Plea.NOT_GUILTY,
                null,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("se11pj")),
                buildEmployerWithAddress(buildAddressObjectWithPostcode(" w1t1jy ")));

        pleadOnlineWith(pleadOnline);

        final Envelope<PleadOnline> newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));

        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(buildPleadOnline(
                Plea.NOT_GUILTY,
                null,
                buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("SE1 1PJ")),
                buildEmployerWithAddress(buildAddressObjectWithPostcode("W1T 1JY"))))));
    }

    @Test
    public void shouldPleadOnlineNotGuiltyWithoutFinances() {
        final PleadOnline pleadOnline = buildPleadOnline(
                Plea.NOT_GUILTY,
                null);

        pleadOnlineWith(pleadOnline);

        final Envelope<PleadOnline> newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));
        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(pleadOnline)));

        verifyZeroInteractions(objectToJsonValueConverter);
    }

    @Test
    public void shouldPleadOnlineNotGuiltyAndWithFinances() {
        final PleadOnline pleadOnline = buildPleadOnline(
                Plea.NOT_GUILTY,
                FinancialMeans.financialMeans().build());

        pleadOnlineWith(pleadOnline);

        final Envelope<PleadOnline> newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));
        assertThat(newCommand.payload(), is(objectToJsonValueConverter.convert(pleadOnline)));
    }

    @Test
    public void shouldPleadOnlineGuiltyAndWithoutFinances() {
        final PleadOnline pleadOnline = buildPleadOnline(
                Plea.GUILTY,
                null);

        exception.expect(BadRequestException.class);
        exception.expectMessage("{\"financialMeans\":[\"Financial Means are required when you are pleading GUILTY\"]}");

        pleadOnlineWith(pleadOnline);
    }

    @Test
    public void shouldPleadOnlineGuiltyAndWithEmptyFinances() {
        final PleadOnline pleadOnline = buildPleadOnline(
                Plea.GUILTY,
                FinancialMeans.financialMeans().build());

        exception.expect(BadRequestException.class);
        exception.expectMessage("{\"financialMeans\":[\"Financial Means are required when you are pleading GUILTY\"]}");

        pleadOnlineWith(pleadOnline);
    }

    @Test
    public void shouldPleadOnlineGuiltyAndWithFinances() {
        final PleadOnline pleadOnline = buildPleadOnline(
                Plea.GUILTY,
                FinancialMeans.financialMeans()
                        .withBenefits(new Benefits(true, true, "Universal Credit"))
                        .withEmploymentStatus("EMPLOYED")
                        .withIncome(new Income(BigDecimal.TEN, Frequency.FORTNIGHTLY))
                        .build());

        pleadOnlineWith(pleadOnline);

        final Envelope<PleadOnline> newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), equalTo(CONTROLLER_PLEAD_ONLINE_COMMAND_NAME));
        assertThat(newCommand.payload(), is(objectToJsonObjectConverter.convert(pleadOnline)));

        verifyZeroInteractions(objectToJsonValueConverter);
    }

    private void pleadOnlineWith(final PleadOnline envelopePayload) {
        final Envelope<PleadOnline> envelope = envelopeFrom(
                metadataWithRandomUUID(PLEAD_ONLINE_COMMAND_NAME),
                envelopePayload);

        pleadOnline.pleadOnline(envelope);

        verify(sender).send(envelopeCaptor.capture());

        verifyZeroInteractions(objectToJsonValueConverter);
    }

}
