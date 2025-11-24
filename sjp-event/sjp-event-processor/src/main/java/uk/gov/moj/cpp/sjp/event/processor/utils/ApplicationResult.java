package uk.gov.moj.cpp.sjp.event.processor.utils;

public enum ApplicationResult {

    STDEC("e2f6e11b-c3a2-4e76-8d29-fbede4174988"),
    ROPENED("e3fb46ee-e406-4f73-9bf1-71d513da8cc7"),

    G("2b3f7c20-8fc1-4fad-9076-df196c24b27e"),
    RFSD("d3902789-4cc8-4753-a15f-7e26dd39f6ae"),
    WDRN("eb2e4c4f-b738-4a4d-9cce-0572cecb7cb8"),

    AACA("177f29cd-49e9-41d1-aafb-5730d7414dd4"),
    AASA("d861586a-df88-440d-98d4-63cc2a680ae1"),

    // Appeal dismissed
    AACD("548bf6b7-d152-4f08-8c9e-a079bf377e9b"),
    AASD("573b195d-5795-43f1-92bb-204de1305b8b"),
    ACSD("3b1f0a20-15cf-4795-98b1-ea87ebab2ec6"),
    ASV("a2640f68-104b-4ef6-9758-56956bd61825"),

    // Appeal Abandoned
    APA("48b8ff83-2d5d-4891-bab1-b0f5edcd3822"),

    // Appeal withdrawn
    AW("453539d1-c1a0-475d-9a02-16a659e6bc34");

    private final String resultId;

    ApplicationResult(final String resultId) {
        this.resultId = resultId;
    }

    public String getResultId() {
        return resultId;
    }


}
