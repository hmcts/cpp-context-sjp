package uk.gov.moj.cpp.sjp.event.processor.service.referral;

public class ReferenceDataNotFoundException extends RuntimeException  {

    public ReferenceDataNotFoundException(String type, String id) {
        super(String.format("Reference data not for %s with id %s",type,id));
    }

}
