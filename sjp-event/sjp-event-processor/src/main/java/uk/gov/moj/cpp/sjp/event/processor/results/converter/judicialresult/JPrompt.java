package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import static java.util.UUID.fromString;

import java.util.UUID;

@SuppressWarnings("squid:S1192")
public enum JPrompt {

    // in future this mapping should be loaded with a reference data call
    // for the uuids to be latest as the codes are unique
    AMOUNT_OF_FINE(fromString("7cd1472f-2379-4f5b-9e67-98a43d86e122"), null),
    AMOUNT_OF_BACK_DUTY(fromString("f7c7c088-f88e-4c28-917c-78571517aca1"), null),
    AMOUNT_OF_EXCISE_PENALTY(fromString("b3dfed9a-efba-4126-a08d-cf37d18b4563"), null),

    FCOMP_MAJOR_CREDITOR(fromString("af921cf4-06e7-4f6b-a4ea-dcb58aab0dbe"), null),
    AMOUNT_OF_COMPENSATION(fromString("26985e5b-fe1f-4d7d-a21a-57207c5966e7"), null),
    FCOST_MAJOR_CREDITOR(fromString("af921cf4-06e7-4f6b-a4ea-dcb58aab0dbe"), null),
    AMOUNT_OF_COSTS(fromString("db261fd9-c6bb-4e10-b93f-9fd98418f7b0"), null),
    AMOUNT_OF_SURCHARGE(fromString("629a971e-9d7a-4526-838d-0a4cb922b5cb"), null),
    DURATION_VALUE_OF_CONDITIONAL_DISCHARGE(fromString("d3205319-84cf-4c5b-9d7a-7e4bb1865054"), null),
    DURATION_UNIT_OF_CONDITIONAL_DISCHARGE(fromString("d3205319-84cf-4c5b-9d7a-7e4bb1865054"), null),
    WDRNNOT_REASONS(fromString("318c9eb2-cf3c-4592-a353-1b2166c15f81"), null),
    PAY_WITHIN_DAYS(fromString("c131cab0-5dd6-11e8-9c2d-fa7ae01bbebc"), null),

    RLSUMI_LUMP_SUM_AMOUNT(fromString("8e235a65-5ea2-4fff-ba3b-6cdb74195436"), null),
    RLSUMI_INSTALMENT_AMOUNT(fromString("1393acda-7a35-4d65-859d-6298e1470cf1"), null),
    RLSUMI_INSTALMENT_START_DATE(fromString("b487696e-dfc9-4c89-80d3-337a4319e925"), null),
    RLSUMI_PAYMENT_FREQUENCY(fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"), null),

    RINSTL_INSTALMENT_START_DATE(fromString("b487696e-dfc9-4c89-80d3-337a4319e925"), null),
    RINSTL_INSTALMENT_AMOUNT(fromString("1393acda-7a35-4d65-859d-6298e1470cf1"), null),
    RINSTL_PAYMENT_FREQUENCY(fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"), null),

    LUMSI_LUMP_SUM_AMOUNT(fromString("8e235a65-5ea2-4fff-ba3b-6cdb74195436"), null),
    LUMSI_INSTALMENT_AMOUNT(fromString("1393acda-7a35-4d65-859d-6298e1470cf1"), null),
    LUMSI_INSTALMENT_START_DATE(fromString("b487696e-dfc9-4c89-80d3-337a4319e925"), null),
    LUMSI_PAYMENT_FREQUENCY(fromString("fb4f761c-29d0-4a8e-a947-3debf281dab0"), null),

    INSTL_INSTALMENT_START_DATE(fromString("b487696e-dfc9-4c89-80d3-337a4319e925"), null),
    INSTALMENTS_AMOUNT(fromString("1393acda-7a35-4d65-859d-6298e1470cf1"), null),
    INSTL_PAYMENT_FREQUENCY(fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"), null),

    REASON_OPTIONAL(fromString("8273d5ba-680e-11e8-adc0-fa7ae01bbebc"), null),
    REASON_FOR_NOT_IMPOSING_OR_REDUCING_VICTIM_SURCHARGE(fromString("042742a1-8d47-4558-9b3e-9f34b358e034"), null),
    REASON_FOR_NO_COMPENSATION(fromString("e263de82-47ca-433a-bb41-cad2e1c5bb72"), null),
    NCOSTS_REASON(fromString("be2a46db-709d-4e0d-9b63-aeb831564c1d"), null),

    EMPLOYEE_REFERENCE_NO(fromString("eb1d0fdc-2e51-4e98-9f6e-ee0daa14c157"), null),
    EMPLOYER_NAME(fromString("485f7d22-718c-4f47-bbd5-f8d934417a03"), "employerOrganisationName"),
    EMPLOYER_ADDRESS_LINE_1(fromString("485f7d22-718c-4f47-bbd5-f8d934417a03"), "employerAddress1"),
    EMPLOYER_ADDRESS_LINE_2(fromString("485f7d22-718c-4f47-bbd5-f8d934417a03"), "employerAddress2"),
    EMPLOYER_ADDRESS_LINE_3(fromString("485f7d22-718c-4f47-bbd5-f8d934417a03"), "employerAddress3"),
    EMPLOYER_ADDRESS_LINE_4(fromString("485f7d22-718c-4f47-bbd5-f8d934417a03"), "employerAddress4"),
    EMPLOYER_ADDRESS_LINE_5(fromString("485f7d22-718c-4f47-bbd5-f8d934417a03"), "employerAddress5"),
    EMPLOYER_POSTCODE(fromString("485f7d22-718c-4f47-bbd5-f8d934417a03"), "employerPostCode"),

    AEOC_REASON(fromString("a289b1bd-06c8-4da3-b117-0bae6017857c"), null),
    LSUM_DATE(fromString("ee7d253a-c629-11e8-a355-529269fb1459"), null),
    COLLECTION_ORDER_TYPE(fromString("6b36e5ff-e116-4dc3-b438-8c02d493959e"), null),
    REASON_NOT_ABD_OR_AEO(fromString("369b6e22-4678-4b04-9fe9-5bb53bed5067"), null),
    ADJOURN_TO_DATE(fromString("185e6a04-8b44-430d-8073-d8d12f69733a"), null),
    SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT(fromString("bca4e07c-17e0-48f1-84f4-7b6ff8bab5e2"), null),
    SUMRTO_DATE_OF_HEARING(fromString("a528bbfd-54b8-45d6-bad0-26a3a95eb274"), null),
    SUMRTO_TIME_OF_HEARING(fromString("4d125a5a-acbc-461d-a657-ba5643af85a6"), null), // latest as on 10-04-2020
    SUMRTO_MAGISTRATES_COURT(fromString("f5699b34-f32f-466e-b7d8-40b4173df154"), null),
    SUMRTO_REASONS_FOR_REFERRING(fromString("dbbb47c9-2202-4913-9a0d-db0a048bfd5f"), null),
    COURT_TO_WHICH_FINE_IS_TRANSFERRED(fromString("5f589095-2986-4d2b-98fa-30ab00f675d4"), null),
    NCOLLO_REASON(fromString("de27ffb3-b7ef-4308-b8c7-ca51ab0c1136"), null),
    LEA_REASON_FOR_PENALTY_POINTS(fromString("bbbb47bb-3418-463c-bfc3-43c6f72bb7c9"), null),
    LEP_PENALTY_POINTS(fromString("a8719de4-7783-448a-b792-e3f94e670ad0"), null),
    DRIVING_LICENCE_NUMBER(fromString("a593ae4a-9d69-45b9-9585-c11aeed28404"), null),

    DDD_DISQUALIFICATION_PERIOD(fromString("2bf54447-328c-4c1b-a123-341adbd52172"), null),
    DDO_DISQUALIFICATION_PERIOD(fromString("2bf54447-328c-4c1b-a123-341adbd52172"), null),
    DDP_DISQUALIFICATION_PERIOD(fromString("2bf54447-328c-4c1b-a123-341adbd52172"), null),

    DDP_NOTIONAL_PENALTY_POINTS(fromString("462d5ba8-7a1c-44a0-a732-6c75601bd6af"), null),
    D45_PRESS_RESTRICTION_APPLIED(fromString("03983f51-f937-4dd8-9656-276b1ca86785"), null),
    DPR_PRESS_RESTRICTION_REVOKED(fromString("1a7da720-a95a-46e4-b2ee-6b8e9db430cc"), null),

    // application result prompts
    STAT_DEC_STATUTORY_DECLARATION_MADE_UNDER(fromString("7b6ef0ac-2d0b-44e5-a68e-4720b96cc679"), null),
    STAT_DEC_FINANCIAL_PENALTIES_TOBE_WRITTEN_OF(fromString("36e32c50-dbec-41c4-904b-25eb20c9c54a"), null),
    STAT_DEC_ORIGINAL_CASE_REFERENCE(fromString("28bea132-dd7a-42d1-a1cc-bcd50ce49e67"), null),
    STAT_DEC_VEHICLE_REGISTRATION_MARK(fromString("9c57c174-e939-49e9-8e14-f4ecda7d6483"), null),
    STAT_DEC_SERVICE_ACCEPTED_OUTSIDE_OF_21_DAYS_LIMIT(fromString("e9f6ff89-8220-4259-88da-f4c26e1f8c77"), null),

    REOPENED_UNDER(fromString("3872ea68-92d9-469f-9929-c676745982ef"), null),
    REOPENED_FINANCIAL_PENALTIES_TOBE_WRITTEN_OF(fromString("36e32c50-dbec-41c4-904b-25eb20c9c54a"), null),
    REOPENED_ORIGINAL_CASE_REFERENCE(fromString("9d1bd249-6fc0-40a3-bdd5-d983022c54d1"), null),
    REOPENED_VEHICLE_REGISTRATION_MARK(fromString("9c57c174-e939-49e9-8e14-f4ecda7d6483"), null),

    REOPENED_CONVICTION_AND_SENTENCE_IMPOSED_AT_SET_ASIDE(fromString("5797feb0-7678-4193-ad16-944365e9a68a"), null),
    REOPENED_SENTENCE_IMPOSED_AT_SET_ASIDE(fromString("beb6bcb8-544c-4b83-943f-167d391e3ebd"), null),

    PROSECUTOR_TOBE_NOTIFIED(fromString("0a7dd6aa-f7cc-43d7-ba03-c9621f3e4471"), null),

    DER_ORIGINAL_COURT_CODE(fromString("b49bf4fa-0b1e-4079-b85a-c0113a46b91b"), null),
    DER_ORIGINAL_CONVICTION_DATE(fromString("0cd86af5-10d8-4fd1-a78a-8a0e80179034"), null),
    DER_DVLA_ENDORSEMENT_CODE(fromString("1a2a23ea-c70d-4a5c-afa7-09dc514f84cc"), null),
    DER_ORIGINAL_OFFENCE_DATE(fromString("37ee2b08-1950-4b04-bea5-f6f66b768bb0"), null),

    DER_ORIGINAL_COURT_CODE2(fromString("1a1e18f9-3278-4f68-98f9-05ab461426f4"), null),
    DER_ORIGINAL_CONVICTION_DATE2(fromString("67f2500c-41e4-4546-b6ae-b17baa995dfd"), null),
    DER_DVLA_ENDORSEMENT_CODE2(fromString("77653f51-5363-4f8d-a732-89a695b29dc2"), null),
    DER_ORIGINAL_OFFENCE_DATE2(fromString("baaf944d-e5bf-466e-a6f3-3de3e5dc36eb"), null),

    DER_ORIGINAL_COURT_CODE3(fromString("65edb93e-c481-4714-b2b5-b29c3451b4e4"), null),
    DER_ORIGINAL_CONVICTION_DATE3(fromString("434da45b-9963-412f-894b-dc66022e2954"), null),
    DER_DVLA_ENDORSEMENT_CODE3(fromString("710363b6-72f8-4a71-9524-e2e529681949"), null),
    DER_ORIGINAL_OFFENCE_DATE3(fromString("c7170ad3-c221-4f26-88fc-8d199d3d9b8c"), null),

    DER_ORIGINAL_COURT_CODE4(fromString("98b4ecd8-56b2-491c-8617-78270c3b8f2c"), null),
    DER_ORIGINAL_CONVICTION_DATE4(fromString("7c1e7cbd-3fad-46f8-8c3e-e49b6503761b"), null),
    DER_DVLA_ENDORSEMENT_CODE4(fromString("7e587ed2-6408-4520-af0d-adb90ac1c620"), null),
    DER_ORIGINAL_OFFENCE_DATE4(fromString("917a319b-4263-4b62-91f7-f354abe9ecdc"), null),

    RFSD_REASONS(fromString("318c9eb2-cf3c-4592-a353-1b2166c15f81"), null);

    private final UUID id;
    private final String promptReference;

    JPrompt(final UUID id, final String promptReference) {
        this.id = id;
        this.promptReference = promptReference;
    }

    public UUID getId() {
        return id;
    }

    public String  getPromptReference() {
        return promptReference;
    }

}
