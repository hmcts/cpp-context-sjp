package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.time.LocalDate;

public final class NextHearing implements Serializable {


    private static final long serialVersionUID = -548572842575474989L;
    private final String courtID;
    private final String courtRoom;
    private final String typeOfCourt;
    private final LocalDate dateAndTime;

    public NextHearing(String courtID,
                       String courtRoom,
                       String typeOfCourt,
                       LocalDate dateAndTime) {
        this.courtID = courtID;
        this.courtRoom = courtRoom;
        this.typeOfCourt = typeOfCourt;
        this.dateAndTime = dateAndTime;

    }

    public String getCourtID() {
        return courtID;
    }
    public String getCourtRoom() {
        return courtRoom;
    }
    public String getTypeOfCourt() {
        return typeOfCourt;
    }
    public LocalDate getDateAndTime() {
        return dateAndTime;
    }
}
