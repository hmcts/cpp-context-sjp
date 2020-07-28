package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.UUID.fromString;

import uk.gov.moj.cpp.sjp.query.view.converter.prompts.EmployerPromptConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.prompts.FixedListPromptConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.prompts.HardcodedValuePromptConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.prompts.LsumDatePromptConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.prompts.MajorCreditorPromptConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.prompts.NoActionPromptConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.prompts.PromptConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.FixedList;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.UUID;
import java.util.function.Supplier;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public enum Prompt {

    AMOUNT_OF_FINE(1, fromString("7cd1472f-2379-4f5b-9e67-98a43d86e122"), PromptConverter::new),
    AMOUNT_OF_BACK_DUTY(1, fromString("f7c7c088-f88e-4c28-917c-78571517aca1"), PromptConverter::new),
    AMOUNT_OF_EXCISE_PENALTY(1, fromString("b3dfed9a-efba-4126-a08d-cf37d18b4563"), PromptConverter::new),

    FCOMP_MAJOR_CREDITOR(1, fromString("af921cf4-06e7-4f6b-a4ea-dcb58aab0dbe"), MajorCreditorPromptConverter::new),
    AMOUNT_OF_COMPENSATION(1, fromString("26985e5b-fe1f-4d7d-a21a-57207c5966e7"), PromptConverter::new),
    FCOST_MAJOR_CREDITOR(1, fromString("af921cf4-06e7-4f6b-a4ea-dcb58aab0dbe"), MajorCreditorPromptConverter::new),
    AMOUNT_OF_COSTS(1, fromString("db261fd9-c6bb-4e10-b93f-9fd98418f7b0"), PromptConverter::new),
    AMOUNT_OF_SURCHARGE(1, fromString("629a971e-9d7a-4526-838d-0a4cb922b5cb"), PromptConverter::new),
    DURATION_VALUE_OF_CONDITIONAL_DISCHARGE(1, fromString("d3205319-84cf-4c5b-9d7a-7e4bb1865054"), NoActionPromptConverter::new),
    DURATION_UNIT_OF_CONDITIONAL_DISCHARGE(2, fromString("d3205319-84cf-4c5b-9d7a-7e4bb1865054"), NoActionPromptConverter::new),
    WDRNNOT_REASONS(2, fromString("1d20aad5-9dc2-42a2-9498-5687f3e5ce33"), PromptConverter::new),
    PAY_WITHIN_DAYS(6, fromString("c131cab0-5dd6-11e8-9c2d-fa7ae01bbebc"), () -> new FixedListPromptConverter(FixedList.PAY_WITHIN_DAYS)),
    RLSUMI_LUMP_SUM_AMOUNT(96, fromString("8e235a65-5ea2-4fff-ba3b-6cdb74195436"), PromptConverter::new),
    RLSUMI_INSTALMENT_AMOUNT(98, fromString("1393acda-7a35-4d65-859d-6298e1470cf1"), PromptConverter::new),
    RLSUMI_INSTALMENT_START_DATE(99, fromString("b487696e-dfc9-4c89-80d3-337a4319e925"), PromptConverter::new),
    RLSUMI_PAYMENT_FREQUENCY(100, fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"), () -> new FixedListPromptConverter(FixedList.INSTALMENTS_PAYMENT_FREQUENCY)),
    RINSTL_INSTALMENT_AMOUNT(96, fromString("1393acda-7a35-4d65-859d-6298e1470cf1"), PromptConverter::new),
    RINSTL_PAYMENT_FREQUENCY(97, fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"), () -> new FixedListPromptConverter(FixedList.INSTALMENTS_PAYMENT_FREQUENCY)),
    RINSTL_INSTALMENT_START_DATE(98, fromString("e091af2e-43d0-495d-b3b0-432010358a45"), PromptConverter::new),
    LSUMI_INSTALMENT_AMOUNT(98, fromString("1393acda-7a35-4d65-859d-6298e1470cf1"), PromptConverter::new),
    LSUMI_PAYMENT_FREQUENCY(100, fromString("fb4f761c-29d0-4a8e-a947-3debf281dab0"), () -> new FixedListPromptConverter(FixedList.INSTALMENTS_PAYMENT_FREQUENCY)),
    LSUMI_INSTALMENT_START_DATE(99, fromString("e091af2e-43d0-495d-b3b0-432010358a45"), PromptConverter::new),
    LSUMI_LUMP_SUM_AMOUNT(96, fromString("11ba492a-e2ad-11e8-9f32-f2801f1b9fd1"), PromptConverter::new),
    INSTALMENTS_AMOUNT(96, fromString("1393acda-7a35-4d65-859d-6298e1470cf1"), PromptConverter::new),
    INSTL_PAYMENT_FREQUENCY(97, fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"), () -> new FixedListPromptConverter(FixedList.INSTALMENTS_PAYMENT_FREQUENCY)),
    INSTL_INSTALMENT_START_DATE(98, fromString("e091af2e-43d0-495d-b3b0-432010358a45"), PromptConverter::new),
    REASON_OPTIONAL(2, fromString("8273d5ba-680e-11e8-adc0-fa7ae01bbebc"), PromptConverter::new),
    REASON_FOR_NOT_IMPOSING_OR_REDUCING_VICTIM_SURCHARGE(10, fromString("042742a1-8d47-4558-9b3e-9f34b358e034"), PromptConverter::new),
    REASON_FOR_NO_COMPENSATION(1, fromString("e263de82-47ca-433a-bb41-cad2e1c5bb72"), PromptConverter::new),
    NCOSTS_REASON(5, fromString("be2a46db-709d-4e0d-9b63-aeb831564c1d"), PromptConverter::new),
    EMPLOYER_NAME(10, fromString("485f7d22-718c-4f47-bbd5-f8d934417a03"), () -> new EmployerPromptConverter("name")),
    EMPLOYEE_REFERENCE_NO(10, fromString("eb1d0fdc-2e51-4e98-9f6e-ee0daa14c157"), () -> new EmployerPromptConverter("employeeReference")),
    EMPLOYER_ADDRESS_LINE_1(10, fromString("86854563-b404-4cd4-9c05-50f1e61a0bfe"), () -> new EmployerPromptConverter("address.address1")),
    EMPLOYER_ADDRESS_LINE_2(10, fromString("ef87b9fe-1d9d-46a2-b094-ce5ba24a9835"), () -> new EmployerPromptConverter("address.address2")),
    EMPLOYER_ADDRESS_LINE_3(10, fromString("df1d55f8-29dd-42d2-8f99-cb4d369cb38b"), () -> new EmployerPromptConverter("address.address3")),
    EMPLOYER_ADDRESS_LINE_4(10, fromString("d3d8e5dc-4ced-4d57-bcbb-5de8f91a580a"), () -> new EmployerPromptConverter("address.address4")),
    EMPLOYER_ADDRESS_LINE_5(10, fromString("a812f1c7-96df-4d8d-9684-25e4e227b2e2"), () -> new EmployerPromptConverter("address.address5")),
    EMPLOYER_POSTCODE(10, fromString("4ff32fe8-5508-4b7e-8e6f-b3e79763a9fe"), () -> new EmployerPromptConverter("address.postcode")),
    AEOC_REASON(10, fromString("a289b1bd-06c8-4da3-b117-0bae6017857c"), PromptConverter::new),
    LSUM_DATE(6, fromString("ee7d253a-c629-11e8-a355-529269fb1459"), LsumDatePromptConverter::new),
    COLLECTION_ORDER_TYPE(4, fromString("6b36e5ff-e116-4dc3-b438-8c02d493959e"), () -> new FixedListPromptConverter(FixedList.COLLECTION_ORDER_TYPE)),
    REASON_NOT_ABD_OR_AEO(9, fromString("369b6e22-4678-4b04-9fe9-5bb53bed5067"), PromptConverter::new),
    ADJOURN_TO_DATE(-1, fromString("185e6a04-8b44-430d-8073-d8d12f69733a"), PromptConverter::new),
    SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT(-1, fromString("bca4e07c-17e0-48f1-84f4-7b6ff8bab5e2"), () -> new FixedListPromptConverter(FixedList.SUMRCC_REFERRAL_REASONS)),
    SUMRTO_DATE_OF_HEARING(5, fromString("a528bbfd-54b8-45d6-bad0-26a3a95eb274"), PromptConverter::new),
    SUMRTO_TIME_OF_HEARING(10, fromString("bf63b0b7-9d4b-45af-a16d-4aa520e6be35"), PromptConverter::new),
    SUMRTO_MAGISTRATES_COURT(15, fromString("f5699b34-f32f-466e-b7d8-40b4173df154"), PromptConverter::new),
    SUMRTO_REASONS_FOR_REFERRING(35, fromString("dbbb47c9-2202-4913-9a0d-db0a048bfd5f"), PromptConverter::new),
    COURT_TO_WHICH_FINE_IS_TRANSFERRED(1, fromString("5f589095-2986-4d2b-98fa-30ab00f675d4"), PromptConverter::new),
    NCOLLO_REASON(0, fromString("de27ffb3-b7ef-4308-b8c7-ca51ab0c1136"), () -> new HardcodedValuePromptConverter("impracticable or inappropriate")),
    LEA_REASON_FOR_PENALTY_POINTS(1, fromString("bbbb47bb-3418-463c-bfc3-43c6f72bb7c9"), PromptConverter::new),
    LEP_PENALTY_POINTS(1, fromString("a8719de4-7783-448a-b792-e3f94e670ad0"), PromptConverter::new),
    DDD_DISQUALIFICATION_PERIOD(1, fromString("2bf54447-328c-4c1b-a123-341adbd52172"), PromptConverter::new),
    DDO_DISQUALIFICATION_PERIOD(1, fromString("2bf54447-328c-4c1b-a123-341adbd52172"), PromptConverter::new),
    DDP_DISQUALIFICATION_PERIOD(1, fromString("2bf54447-328c-4c1b-a123-341adbd52172"), PromptConverter::new),
    DDP_NOTIONAL_PENALTY_POINTS(2, fromString("462d5ba8-7a1c-44a0-a732-6c75601bd6af"), PromptConverter::new),
    D45_PRESS_RESTRICTION_APPLIED(1, fromString("03983f51-f937-4dd8-9656-276b1ca86785"), PromptConverter::new),
    DPR_PRESS_RESTRICTION_REVOKED(1, fromString("1a7da720-a95a-46e4-b2ee-6b8e9db430cc"), PromptConverter::new);

    private final Integer index;
    private final UUID id;

    private final Supplier<PromptConverter> promptConverter;

    Prompt(final Integer index, final UUID id, final Supplier<PromptConverter> promptConverter) {
        this.index = index;
        this.id = id;
        this.promptConverter = promptConverter;
    }


    public UUID getId() {
        return id;
    }

    public Integer getIndex() {
        return index;
    }

    public void createPrompt(final JsonArrayBuilder promptsPayloadBuilder, final JsonObject terminalEntry, final OffenceDataSupplier offenceDataSupplier) {
        promptConverter.get().createPrompt(promptsPayloadBuilder, terminalEntry, this, offenceDataSupplier);
    }
}
