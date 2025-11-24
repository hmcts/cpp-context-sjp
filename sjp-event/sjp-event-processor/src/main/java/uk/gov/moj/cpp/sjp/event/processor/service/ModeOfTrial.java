package uk.gov.moj.cpp.sjp.event.processor.service;


public enum ModeOfTrial {
    //This is just a mock data. Todo replace the 2 digit code as agreed by the business . See subtask CRC-10710 and CRC-10793
    EWAY(11),
    SIMP(22),
    IND(33),
    SNONIMP(44);

    private Integer twoDigitCode;

    ModeOfTrial(final Integer digitCode){
        this.twoDigitCode = digitCode;
    }

    public Integer getTwoDigitCode() {
        return twoDigitCode;
    }

}

