package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import static java.util.UUID.fromString;

import java.util.UUID;

public enum JResultCode {

    // in future this mapping should be loaded with a reference data call
    // for the uuids to be latest as the codes are unique
    FVEBD(fromString("5edd3a3a-8dc7-43e4-96c4-10fed16278ac")),
    EXPEN(fromString("fcb26a5f-28cc-483e-b430-d823fac808df")),
    FO(fromString("969f150c-cd05-46b0-9dd9-30891efcc766")),
    FCOMP(fromString("ae89b99c-e0e3-47b5-b218-24d4fca3ca53")),
    FCOST(fromString("76d43772-0660-4a33-b5c6-8f8ccaf6b4e3")),
    FVS(fromString("e866cd11-6073-4fdf-a229-51c9d694e1d0")),
    GPTAC(fromString("9161f3cb-e821-44e5-a9ee-4680b358a037")),
    CD(fromString("554c2622-c1cc-459e-a98d-b7f317ab065c")),
    WDRNNOT(fromString("6feb0f2e-8d1e-40c7-af2c-05b28c69e5fc")),
    RLSUM(fromString("a09bbfa0-5dd5-11e8-9c2d-fa7ae01bbebc")),
    RLSUMI(fromString("d6e93aae-5dd7-11e8-9c2d-fa7ae01bbebc")),
    RINSTL(fromString("9ba8f03a-5dda-11e8-9c2d-fa7ae01bbebc")),
    LSUM(fromString("bcb5a496-f7cf-11e8-8eb2-f2801f1b9fd1")), // PAY BY DATE
    LUMSI(fromString("272d1ec2-634b-11e8-adc0-fa7ae01bbebc")),
    INSTL(fromString("6d76b10c-64c4-11e8-adc0-fa7ae01bbebc")),
    ABDC(fromString("f7dfefd2-64c6-11e8-adc0-fa7ae01bbebc")),
    NOVS(fromString("204fc6b8-d6c9-4fb8-acd0-47d23c087625")),
    NCR(fromString("29e02fa1-42ce-4eec-914e-e62508397a16")),
    NCOSTS(fromString("baf94928-04ae-4609-8e96-efc9f081b2be")),
    AEOC(fromString("bdb32555-8d55-4dc1-b4b6-580db5132496")),
    AD(fromString("b9c6047b-fb84-4b12-97a1-2175e4b8bbac")),
    COLLO(fromString("9ea0d845-5096-44f6-9ce0-8ae801141eac")),
    NCOLLO(fromString("615313b5-0647-4d61-b7b8-6b36265d8929")),
    D(fromString("14d66587-8fbe-424f-a369-b1144f1684e3")),// actual code is DISM ??
    LEN(fromString("b0aeb4fc-df63-4e2f-af88-97e3f23e847f")),
    LEA(fromString("3fa139cc-efe0-422b-93d6-190a5be50953")),
    LEP(fromString("cee54856-4450-4f28-a8a9-72b688726201")),
    DDD(fromString("ccfc452e-ebe4-4cd7-b8a0-4f90768447b4")),
    DDO(fromString("b2d06bbc-e90e-4df8-8851-6e4a70894828")),
    DDP(fromString("73fe22ca-76bd-4aba-bdea-6dfef8ee03a2")),
    NSP(fromString("49939c7c-750f-403e-9ce1-f82e3e568065")),
    TFOOUT(fromString("1e96d1a9-9618-4ddd-a925-ca6a0ef86018")),
    SUMRCC(fromString("600edfc3-a584-4f9f-a52e-5bb8a99646c1")),
    ADJOURNSJP(fromString("f7784e82-20b5-4d2c-b174-6fd57ebf8d7c")),
    SUMRTO(fromString("3d2c05b3-fcd6-49c2-b5a9-52855be7f90a")),
    SJPR(fromString("0149ab92-5466-11e8-9c2d-fa7ae01bbebc")),
    D45(fromString("fcbf777d-1a73-47e7-ab9b-7c51091a022c")),
    DPR(fromString("b27b42bf-e20e-46ec-a6e3-5c2e8a076c20")),
    SETASIDE(fromString("af590f98-21cb-43e7-b992-2a9d444acb2b")),

    // application result codes
    STDEC(fromString("e2f6e11b-c3a2-4e76-8d29-fbede4174988")),
    ROPENED(fromString("e3fb46ee-e406-4f73-9bf1-71d513da8cc7")),
    DER(fromString("f42fa098-0f7b-4269-ac4f-b10c9b6832d7")),
    RFSD(fromString("d3902789-4cc8-4753-a15f-7e26dd39f6ae"));

    private final UUID resultDefinitionId;

    JResultCode(final UUID resultDefinitionId) {
        this.resultDefinitionId = resultDefinitionId;
    }

    public UUID getResultDefinitionId() {
        return resultDefinitionId;
    }
}
