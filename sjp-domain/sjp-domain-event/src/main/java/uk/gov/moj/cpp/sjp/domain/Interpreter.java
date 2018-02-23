package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Interpreter implements Serializable {

    private static final long serialVersionUID = 5307681171888861760L;

    private String language;

    public Interpreter() {
    }

    public Interpreter(String language) {
        setLanguage(language);
    }

    public Boolean getNeeded() {
        return ! StringUtils.isEmpty(language);
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

}
