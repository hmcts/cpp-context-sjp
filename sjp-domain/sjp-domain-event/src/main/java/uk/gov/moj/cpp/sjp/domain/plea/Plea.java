package uk.gov.moj.cpp.sjp.domain.plea;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public class Plea {

    @JsonIgnore
    private Type type;

    @JsonIgnore
    private String offenceId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private GuiltyPlea guiltyPlea;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private NotGuiltyPlea notGuiltyPlea;

    public Plea() {
        //default constructor
    }

    public Plea(Type type, String offenceId, GuiltyPlea guiltyPlea, NotGuiltyPlea notGuiltyPlea) {
        this.type = type;
        this.offenceId = offenceId;
        this.guiltyPlea = guiltyPlea;
        this.notGuiltyPlea = notGuiltyPlea;
    }

    public Type getType() {
        return type;
    }

    public String getOffenceId() {
        return offenceId;
    }

    public GuiltyPlea getGuiltyPlea() {
        return guiltyPlea;
    }

    public NotGuiltyPlea getNotGuiltyPlea() {
        return notGuiltyPlea;
    }

    public enum Type {

        GUILTY("GUILTY"),
        NOT_GUILTY("NOT_GUILTY"),
        GUILTY_REQUEST_HEARING("GUILTY_REQUEST_HEARING"),
        NO_PLEA("NO_PLEA"),
        ACTUAL_GUILTY("ACTUAL_GUILTY");

        private String name;

        Type(String name) {
            this.name = name;
        }

        public static Type fromString(String s) {
            for (Type type : values()) {
                if (type.name.equals(s)) {
                    return type;
                }
            }
            return null;
        }
    }
}
