package uk.gov.moj.cpp.sjp.domain.plea;


public class LanguageInterpreter {
    private String language;

    public LanguageInterpreter() {
        //default constructor
    }

    public LanguageInterpreter(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

}
