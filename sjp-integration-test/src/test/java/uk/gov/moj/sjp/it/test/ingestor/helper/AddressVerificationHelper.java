package uk.gov.moj.sjp.it.test.ingestor.helper;

import uk.gov.moj.sjp.it.command.CreateCase;

public class AddressVerificationHelper {
    public static final String SPACE = " ";


    public static String addressLinesFrom(final CreateCase.DefendantBuilder defendantBuilder) {

        final String addressLineOne = defendantBuilder.getAddressBuilder().getAddress1();
        final String addressLineTwo = defendantBuilder.getAddressBuilder().getAddress2();
        final String addressLineThree = defendantBuilder.getAddressBuilder().getAddress3();
        final String addressLineFour = defendantBuilder.getAddressBuilder().getAddress4();
        final String addressLineFive = defendantBuilder.getAddressBuilder().getAddress5();

        return new StringBuilder(addressLineOne).append(SPACE)
                .append(addressLineTwo)
                .append(SPACE)
                .append(addressLineThree)
                .append(SPACE)
                .append(addressLineFour)
                .append(SPACE)
                .append(addressLineFive).toString();
    }

}
