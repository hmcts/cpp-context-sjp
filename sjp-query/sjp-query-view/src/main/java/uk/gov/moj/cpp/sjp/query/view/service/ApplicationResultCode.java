package uk.gov.moj.cpp.sjp.query.view.service;

public enum ApplicationResultCode {

    STDEC("e2f6e11b-c3a2-4e76-8d29-fbede4174988"),

    ROPENED("e3fb46ee-e406-4f73-9bf1-71d513da8cc7"),

    DER("f42fa098-0f7b-4269-ac4f-b10c9b6832d7"),

    RFSD("d3902789-4cc8-4753-a15f-7e26dd39f6ae");

    private String resultDefinitionId;

    ApplicationResultCode(final String resultDefinitionId) {
        this.resultDefinitionId = resultDefinitionId;
    }

    public String getResultDefinitionId() {
        return resultDefinitionId;
    }
}
