package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

public enum ConversionFormat {

    PDF("pdf"),
    DOC("doc"),
    DOCX("docx");

    private final String value;

    ConversionFormat(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
