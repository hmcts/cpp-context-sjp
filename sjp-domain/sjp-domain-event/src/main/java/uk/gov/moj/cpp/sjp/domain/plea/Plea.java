package uk.gov.moj.cpp.sjp.domain.plea;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public class Plea {

    @JsonIgnore
    private final PleaType type;

    @JsonIgnore
    private final String offenceId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final GuiltyPlea guiltyPlea;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final NotGuiltyPlea notGuiltyPlea;

    public Plea() {
        this(null, null, null, null);
    }

    public Plea(PleaType type, String offenceId, GuiltyPlea guiltyPlea, NotGuiltyPlea notGuiltyPlea) {
        this.type = type;
        this.offenceId = offenceId;
        this.guiltyPlea = guiltyPlea;
        this.notGuiltyPlea = notGuiltyPlea;
    }

    public PleaType getType() {
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

}
