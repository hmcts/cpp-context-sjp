package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getBoolean;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.EXPECTED_DATE_READY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.POSTING_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.URN;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PROSECUTOR_AOCP_APPROVED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.processor.service.AzureFunctionService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseReceivedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReceivedProcessor.class);
    public static final String CASE_STARTED_PUBLIC_EVENT_NAME = "public.sjp.sjp-case-created";
    public static final String RESOLVE_CASE_AOCP_ELIGIBILITY = "sjp.command.resolve-case-aocp-eligibility";
    private static final String FINE = "Fine";
    private static final String ORGANISATION = "Organisation";
    private static final String YOUTH = "Youth";
    private static final String ADULT = "Adult";
    private static final String SURCHARGE_AMOUNT = "surchargeAmount";
    private static final String SURCHARGE_AMOUNT_MIN = "surchargeAmountMin";
    private static final String SURCHARGE_AMOUNT_MAX = "surchargeAmountMax";
    private static final String SURCHARGE_FINE_PERCENTAGE = "surchargeFinePercentage";

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    protected Sender sender;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private TimerService timerService;

    @Inject
    private AzureFunctionService azureFunctionService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Handles(CaseReceived.EVENT_NAME)
    public void handleCaseReceivedEvent(final JsonEnvelope event) {
        final UUID caseId = UUID.fromString(event.payloadAsJsonObject().getString(CASE_ID));
        final String caseUrn = event.payloadAsJsonObject().getString(URN);
        final LocalDate postingDate = LocalDate.parse(event.payloadAsJsonObject().getString(POSTING_DATE));
        final LocalDate expectedDateReady = LocalDate.parse(event.payloadAsJsonObject().getString(EXPECTED_DATE_READY));
        final String prosecutingAuthority = event.payloadAsJsonObject().getString(PROSECUTING_AUTHORITY);
        final Defendant defendant =  jsonObjectConverter.convert(event.payloadAsJsonObject().getJsonObject(DEFENDANT), Defendant.class);

        resolveCaseAOCPEligibility(event, caseId, prosecutingAuthority, defendant, postingDate);

        raisePublicEvent(event.metadata(), caseId, postingDate);
        relayCaseToCourtStore(caseUrn);

        //Does activity and JMS share the same transaction?
        timerService.startTimerForDefendantResponse(caseId, expectedDateReady, event.metadata());
    }

    private void resolveCaseAOCPEligibility(final JsonEnvelope event, final UUID caseId, final String prosecutingAuthority,
                                            final Defendant defendant, final LocalDate postingDate) {

        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add(CASE_ID, caseId.toString());

        final Optional<JsonObject> prosecutorDetails = referenceDataService.getProsecutor(prosecutingAuthority, event);
        if (prosecutorDetails.isPresent()) {
            final Optional<Boolean> isProsecutorAOCApproved = getBoolean(prosecutorDetails.get(), "aocpApproved");
            payloadBuilder.add(PROSECUTOR_AOCP_APPROVED, isProsecutorAOCApproved.orElse(false));
            populateVictimSurcharge(event, defendant, postingDate, payloadBuilder);


            sender.send(envelopeFrom(JsonEnvelope.metadataFrom(event.metadata()).withName(RESOLVE_CASE_AOCP_ELIGIBILITY).build(),
                    payloadBuilder.build()));
        }
    }

    private void raisePublicEvent(final Metadata metadata, final UUID caseId, final LocalDate postingDate) {
        final Metadata publicEventMetadata = metadataFrom(metadata)
                .withName(CASE_STARTED_PUBLIC_EVENT_NAME)
                .build();

        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add("id", caseId.toString())
                .add("postingDate", postingDate.toString())
                .build();
        sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
    }

    private void relayCaseToCourtStore(String caseId) {

        if (!caseId.isEmpty()) {
            final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
            payloadBuilder.add("CaseReference", caseId);
            try {
                this.azureFunctionService.relayCaseOnCPP(payloadBuilder.build().toString());
            } catch (IOException ex) {
                LOGGER.error("Error relaying case to court store.",ex);
           }
        }
    }

    private void populateVictimSurcharge(final JsonEnvelope event, final Defendant defendant, final LocalDate postingDate,
                                         final JsonObjectBuilder payloadBuilder) {
        final String surchargeLevel = getSurchargeLevel(defendant, postingDate);
        final Optional<JsonObject> victimSurcharge = referenceDataService.getVictimSurcharges(event, requester, FINE, surchargeLevel).stream()
                .findFirst();

        if (!victimSurcharge.isPresent()) {
            payloadBuilder.add(SURCHARGE_AMOUNT, BigDecimal.ZERO);
        } else {
            setVictimSurchargeParams(victimSurcharge.get(), SURCHARGE_AMOUNT, payloadBuilder);
            setVictimSurchargeParams(victimSurcharge.get(), SURCHARGE_AMOUNT_MIN, payloadBuilder);
            setVictimSurchargeParams(victimSurcharge.get(), SURCHARGE_AMOUNT_MAX, payloadBuilder);
            setVictimSurchargeParams(victimSurcharge.get(), SURCHARGE_FINE_PERCENTAGE, payloadBuilder);
        }
    }

    private void setVictimSurchargeParams(final JsonObject victimSurcharge, final String fieldName, final JsonObjectBuilder payloadBuilder) {
        if (victimSurcharge.containsKey(fieldName) && !victimSurcharge.isNull(fieldName)) {
            payloadBuilder.add(fieldName, victimSurcharge.getJsonNumber(fieldName).bigDecimalValue());
        }
    }

    private String getSurchargeLevel(final Defendant defendant, final LocalDate postingDate) {
        if (isNotEmpty(defendant.getLegalEntityName())) {
            return ORGANISATION;
        } else if (isYouth(defendant.getDateOfBirth(), postingDate)) {
            return YOUTH;
        } else {
            return ADULT;
        }
    }

    private boolean isYouth(final LocalDate dateOfBirth, final LocalDate postingDate) {
        return nonNull(dateOfBirth) && postingDate.minus(18, ChronoUnit.YEARS).isBefore(dateOfBirth);
    }
}