package uk.gov.moj.cpp.sjp.command.handler;

import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.json.schemas.domains.sjp.commands.CreateCaseApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.handler.service.ReferenceDataService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

@ServiceComponent(COMMAND_HANDLER)
public class CreateCaseApplicationHandler extends CaseCommandHandler {

    private static final String COMMAND_NAME = "sjp.command.handler.create-case-application";
    private static final UUID TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER = UUID.fromString("8b1cff00-a456-40da-9ce4-f11c20959084");
    private static final String WORDING_RESENTENCED = "Resentenced Original code : %1$s, Original details: %2$s";
    private static final String WORDING_SUSPENDED_RESENTENCED = "Activation of a suspended sentence order. Original code : %1$s, Original details: %2$s";
    private static final String PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY = "contactEmailAddress";
    private static final String PROSECUTOR_OUCODE_KEY = "oucode";
    private static final String PROSECUTOR_MAJOR_CREDITOR_CODE_KEY = "majorCreditorCode";

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles(COMMAND_NAME)
    public void createCaseApplication(final Envelope<CreateCaseApplication> createCaseApplicationCommand) throws EventStreamException {

        final CreateCaseApplication createCaseApplicationRequest = rebuildCreateCaseApplication(createCaseApplicationCommand.payload(), createCaseApplicationCommand);
        applyToCaseAggregate(createCaseApplicationRequest.getCaseId(), createCaseApplicationCommand, caseAggregate ->
                caseAggregate.createCaseApplication(
                        createCaseApplicationRequest
                ));
    }

    private CreateCaseApplication rebuildCreateCaseApplication(final CreateCaseApplication createCaseApplication, final Envelope<CreateCaseApplication> envelope) {
        final CreateCaseApplication.Builder createCaseApplicationBuilder = CreateCaseApplication.createCaseApplication()
                .withCaseId(createCaseApplication.getCaseId())
                .withApplicationIdExists(createCaseApplication.getApplicationIdExists())
                .withCourtApplication(rebuildCourtApplication(createCaseApplication.getCourtApplication(), envelope));
        return createCaseApplicationBuilder.build();
    }

    private CourtApplication rebuildCourtApplication(final CourtApplication courtApplication, final Envelope<?> envelope) {

        final CourtApplication.Builder courtApplicationBuilder = CourtApplication.courtApplication()
                .withValuesFrom(courtApplication);

        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), JsonValue.NULL);

        updateClonedCourtApplicationParties(courtApplicationBuilder, courtApplication, jsonEnvelope);
        updateClonedOffenceApplicationCases(courtApplicationBuilder, courtApplication);
        updateClonedOffenceCourtOrder(courtApplicationBuilder, courtApplication);

        return courtApplicationBuilder.build();
    }

    private void updateClonedCourtApplicationParties(final CourtApplication.Builder courtApplicationBuilder, final CourtApplication courtApplication, final JsonEnvelope jsonEnvelope) {

        if (nonNull(courtApplication.getApplicant().getProsecutingAuthority())) {
            courtApplicationBuilder.withApplicant(enrichCourtApplicationParty(courtApplication.getApplicant(), jsonEnvelope));
        }
        if (nonNull(courtApplication.getSubject().getProsecutingAuthority())) {
            courtApplicationBuilder.withSubject(enrichCourtApplicationParty(courtApplication.getSubject(), jsonEnvelope));
        }

        ofNullable(courtApplication.getRespondents())
                .ifPresent(courtApplicationParties -> courtApplicationBuilder.withRespondents(courtApplicationParties.stream()
                        .filter(respondent -> nonNull(respondent.getProsecutingAuthority()))
                        .map(respondent -> enrichCourtApplicationParty(respondent, jsonEnvelope))
                        .collect(toList())));


        final List<CourtApplicationParty> thirdParties = new ArrayList<>();
        ofNullable(courtApplication.getThirdParties()).ifPresent(thirdParties::addAll);

        final boolean prosecutorsAsThirdParty = checkForThirdPartyProsecutors(courtApplication);
        if (prosecutorsAsThirdParty) {
            ofNullable(buildThirdPartiesFromProsecutors(courtApplication)).ifPresent(thirdParties::addAll);
        }

        if (isNotEmpty(thirdParties)) {
            courtApplicationBuilder.withThirdParties(thirdParties.stream()
                    .filter(thirdParty -> nonNull(thirdParty.getProsecutingAuthority()))
                    .map(thirdParty -> enrichCourtApplicationParty(thirdParty, jsonEnvelope))
                    .collect(toList()));
        }
    }

    private List<CourtApplicationParty> buildThirdPartiesFromProsecutors(final CourtApplication courtApplication) {
        final Set<UUID> existingProsecutorIds = new HashSet<>();
        addToExistingProsecutorIds(existingProsecutorIds, Collections.singletonList(courtApplication.getApplicant()));

        if (isNotEmpty(courtApplication.getRespondents())) {
            addToExistingProsecutorIds(existingProsecutorIds, courtApplication.getRespondents());
        }

        if (isNotEmpty(courtApplication.getThirdParties())) {
            addToExistingProsecutorIds(existingProsecutorIds, courtApplication.getThirdParties());
        }

        final List<CourtApplicationParty> thirdParties = new ArrayList<>();

        if (isNotEmpty(courtApplication.getCourtApplicationCases())) {
            courtApplication.getCourtApplicationCases().stream()
                    .map(CourtApplicationCase::getProsecutionCaseIdentifier)
                    .filter(prosecutionCaseIdentifier -> !existingProsecutorIds.contains(prosecutionCaseIdentifier.getProsecutionAuthorityId()))
                    .forEach(prosecutionCaseIdentifier -> addProsecutorToThirdParties(prosecutionCaseIdentifier, thirdParties));
        }

        if (nonNull(courtApplication.getCourtOrder()) && isNotEmpty(courtApplication.getCourtOrder().getCourtOrderOffences())) {
            courtApplication.getCourtOrder().getCourtOrderOffences().stream()
                    .map(CourtOrderOffence::getProsecutionCaseIdentifier)
                    .filter(prosecutionCaseIdentifier -> !existingProsecutorIds.contains(prosecutionCaseIdentifier.getProsecutionAuthorityId()))
                    .forEach(prosecutionCaseIdentifier -> addProsecutorToThirdParties(prosecutionCaseIdentifier, thirdParties));
        }

        return thirdParties.stream().collect(collectingAndThen(toList(), getListOrNull()));
    }

    private void addProsecutorToThirdParties(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final List<CourtApplicationParty> thirdParties) {
        final CourtApplicationParty courtApplicationParty = buildCourtApplicationParty(prosecutionCaseIdentifier);
        thirdParties.add(courtApplicationParty);
    }

    private CourtApplicationParty buildCourtApplicationParty(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(UUID.randomUUID())
                .withSummonsRequired(true)
                .withNotificationRequired(true)
                .withProsecutingAuthority(buildProsecutingAuthority(prosecutionCaseIdentifier))
                .build();
    }

    private ProsecutingAuthority buildProsecutingAuthority(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return prosecutingAuthority()
                .withProsecutionAuthorityId(prosecutionCaseIdentifier.getProsecutionAuthorityId())
                .withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode())
                .withName(prosecutionCaseIdentifier.getProsecutionAuthorityName())
                .withAddress(prosecutionCaseIdentifier.getAddress())
                .withContact(prosecutionCaseIdentifier.getContact())
                .withProsecutorCategory(prosecutionCaseIdentifier.getProsecutorCategory())
                .withMajorCreditorCode(prosecutionCaseIdentifier.getMajorCreditorCode())
                .withProsecutionAuthorityOUCode(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode())
                .build();
    }

    private void addToExistingProsecutorIds(final Set<UUID> existingProsecutorIds, final List<CourtApplicationParty> courtApplicationParties) {
        courtApplicationParties.stream()
                .filter(courtApplicationParty -> nonNull(courtApplicationParty.getProsecutingAuthority()))
                .map(courtApplicationParty -> courtApplicationParty.getProsecutingAuthority().getProsecutionAuthorityId())
                .forEach(existingProsecutorIds::add);
    }

    private boolean checkForThirdPartyProsecutors(final CourtApplication courtApplication) {
        return courtApplication.getType().getProsecutorThirdPartyFlag() && hasApplicationCasesOrCourtOrders(courtApplication);
    }

    private boolean hasApplicationCasesOrCourtOrders(final CourtApplication courtApplication) {
        return isNotEmpty(courtApplication.getCourtApplicationCases()) || nonNull(courtApplication.getCourtOrder());
    }

    private CourtApplicationParty enrichCourtApplicationParty(final CourtApplicationParty courtApplicationParty, final JsonEnvelope jsonEnvelope) {
        final CourtApplicationParty.Builder builder = CourtApplicationParty.courtApplicationParty().withValuesFrom(courtApplicationParty);
        builder.withProsecutingAuthority(fetchProsecutingAuthorityInformation(courtApplicationParty.getProsecutingAuthority(), jsonEnvelope));
        return builder.build();
    }
    @SuppressWarnings("pmd:NullAssignment")
    private ProsecutingAuthority fetchProsecutingAuthorityInformation(final ProsecutingAuthority prosecutingAuthority, final JsonEnvelope jsonEnvelope) {

        final ProsecutingAuthority.Builder prosecutingAuthorityBuilder = prosecutingAuthority().withValuesFrom(prosecutingAuthority);

        final Optional<JsonObject> optionalProsecutorJson = referenceDataService.getProsecutor(jsonEnvelope, prosecutingAuthority.getProsecutionAuthorityId());
        if (optionalProsecutorJson.isPresent()) {
            final JsonObject jsonObject = optionalProsecutorJson.get();
            prosecutingAuthorityBuilder.withName(jsonObject.getString("fullName"))
                    .withWelshName(jsonObject.getString("nameWelsh", null))
                    .withAddress(isNull(jsonObject.getJsonObject("address")) ? null : jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("address"), Address.class));

            if (jsonObject.containsKey(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY)) {
                prosecutingAuthorityBuilder.withContact(contactNumber()
                        .withPrimaryEmail(jsonObject.getString(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY))
                        .build());
            }

            if (jsonObject.containsKey(PROSECUTOR_OUCODE_KEY)) {
                prosecutingAuthorityBuilder.withProsecutionAuthorityOUCode(jsonObject.getString(PROSECUTOR_OUCODE_KEY));
            }

            if (jsonObject.containsKey(PROSECUTOR_MAJOR_CREDITOR_CODE_KEY)) {
                prosecutingAuthorityBuilder.withMajorCreditorCode(jsonObject.getString(PROSECUTOR_MAJOR_CREDITOR_CODE_KEY));
            }
        }
        return prosecutingAuthorityBuilder.build();
    }

    private <T> UnaryOperator<List<T>> getListOrNull() {
        return list -> list.isEmpty() ? null : list;
    }

    private void updateClonedOffenceApplicationCases(final CourtApplication.Builder courtApplicationBuilder, final CourtApplication courtApplication) {
        if (isNull(courtApplication.getCourtApplicationCases())) {
            return;
        }

        if (LinkType.FIRST_HEARING != courtApplication.getType().getLinkType()) {

            final String wordingPattern = getOffenceWordingPattern(courtApplication);

            final String resentencingActivationCode = courtApplication.getType().getResentencingActivationCode();

            final List<CourtApplicationCase> courtApplicationCases = courtApplication.getCourtApplicationCases().stream()
                    .map(courtApplicationCase -> {
                        if (activeCase(courtApplicationCase.getCaseStatus())) {
                            return courtApplicationCase()
                                    .withValuesFrom(courtApplicationCase)
                                    .withOffences(null)
                                    .build();
                        } else {
                            return courtApplicationCase()
                                    .withValuesFrom(courtApplicationCase)
                                    .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                            .map(courtApplicationOffence -> updateOffence(courtApplication.getType(), courtApplicationOffence, wordingPattern, resentencingActivationCode))
                                            .collect(collectingAndThen(toList(), getListOrNull())))
                                    .build();
                        }
                    })
                    .collect(toList());

            courtApplicationBuilder.withCourtApplicationCases(courtApplicationCases);
        }
    }

    private boolean activeCase(final String caseStatus) {
        return nonNull(caseStatus) && !"INACTIVE".equalsIgnoreCase(caseStatus) && !"CLOSED".equalsIgnoreCase(caseStatus);
    }

    private String getOffenceWordingPattern(final CourtApplication courtApplication) {
        final String wordingPattern;
        if (nonNull(courtApplication.getCourtOrder()) && TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER.equals(courtApplication.getCourtOrder().getJudicialResultTypeId())) {
            wordingPattern = WORDING_SUSPENDED_RESENTENCED;
        } else {
            wordingPattern = WORDING_RESENTENCED;
        }
        return wordingPattern;
    }

    private Offence updateOffence(final CourtApplicationType courtApplicationType, final Offence offence, final String wordingPattern, final String resentencingActivationCode) {
        final Offence.Builder offenceBuilder = Offence.offence()
                .withValuesFrom(offence)
                .withJudicialResults(null)
                .withCustodyTimeLimit(null);

        if (!offence.getOffenceCode().equals(resentencingActivationCode) && hasActivationCode(courtApplicationType)) {
            offenceBuilder.withWording(String.format(wordingPattern, offence.getOffenceCode(), offence.getWording()))
                    .withOffenceCode(resentencingActivationCode);

            if (nonNull(offence.getWordingWelsh())) {
                offenceBuilder.withWordingWelsh(String.format(wordingPattern, offence.getOffenceCode(), offence.getWordingWelsh()));
            }
        }

        return offenceBuilder.build();

    }

    private boolean hasActivationCode(final CourtApplicationType type) {
        return StringUtils.isNotEmpty(type.getResentencingActivationCode());
    }

    private void updateClonedOffenceCourtOrder(final CourtApplication.Builder courtApplicationBuilder, final CourtApplication courtApplication) {

        if (isNull(courtApplication.getCourtOrder())) {
            return;
        }

        final String wordingPattern = getOffenceWordingPattern(courtApplication);

        final String resentencingActivationCode = courtApplication.getType().getResentencingActivationCode();

        final CourtOrder newCourtOrder = ofNullable(courtApplication.getCourtOrder())
                .map(courtOrder -> CourtOrder.courtOrder().withValuesFrom(courtOrder)
                        .withCourtOrderOffences(courtOrder.getCourtOrderOffences().stream()
                                .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence().withValuesFrom(courtOrderOffence)
                                        .withOffence(updateOffence(courtApplication.getType(), courtOrderOffence.getOffence(), wordingPattern, resentencingActivationCode))
                                        .build())
                                .collect(toList()))
                        .build())
                .orElse(null);

        courtApplicationBuilder.withCourtOrder(newCourtOrder);
    }
}

