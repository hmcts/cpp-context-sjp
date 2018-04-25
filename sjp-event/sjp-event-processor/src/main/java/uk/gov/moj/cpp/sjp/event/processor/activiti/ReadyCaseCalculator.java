package uk.gov.moj.cpp.sjp.event.processor.activiti;

public class ReadyCaseCalculator {

    private ReadyCaseCalculator() {
    }

    public static boolean isCaseReady(boolean provedInAbsence, boolean pleaReceived, boolean withdrawalRequested) {
        return provedInAbsence || pleaReceived || withdrawalRequested;
    }

}
