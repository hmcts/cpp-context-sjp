package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Embeddable
public class InterpreterDetail implements Serializable{

    private static final long serialVersionUID = 6565253161566539015L;

    @Column(name="interpreter_language")
    private String language;

    public InterpreterDetail(String language){
        setLanguage(language);
    }
    
    public InterpreterDetail(){
    }

    public String getLanguage() {
        return language;
    }

    public Boolean getNeeded() {
        return ! StringUtils.isEmpty(language);
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
    
}
