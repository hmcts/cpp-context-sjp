package uk.gov.moj.cpp.sjp.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

public class DomainUtilsTest {

    private static final LocalDate now = LocalDate.now();
    private static final String[] stringArrayOne = {"address1", "address2", "address3", "address4", "address5", "postcode"};
    private static final String[] stringsArrayTwo = {"address1", "address2", "address3", null, "", "postcode"};
    private static final String[] stringArrayThree = {"address1", "address2", "address3", "", null, "postcode"};

    private static final Object[] ObjectArrayOne = {"address1", "address2", "address3", "address4", "address5", "postcode", now};
    private static final Object[] ObjectArrayTwo = {"address1", "address2", "address3", null, "", "postcode", now};
    private static final Object[] ObjectArrayThree = {"address1", "address2", "address3", "", null, "postcode", now};

    private static final Address firstAddress = new Address(
            "address1", "address2", "address3",
            "address4", "address5", "postcode");

    private static final Address secondAddress = new Address(
            "address1", "address2", "address3",
            "address4", "address5", "postcode");

    private static final Address addressOne = new Address(
            "address1", "address2", "address3",
            "", null, "postcode");

    private static final Address addressTwo = new Address(
            "address1", "address2", "address3",
            null, "", "postcode");

    private static final Address aAddress = new Address(
            "address1", "this is not address2", "address3",
            "address4", null, "postcode");

    private static final Address bAddress = new Address(
            "address", "address2", "address3",
            "address4", "", "postcode");

    @Test
    public void shouldComparingTwoSameAddressesReturnTrue() {
        assertTrue(firstAddress.equals(secondAddress));
    }

    @Test
    public void shouldBeSameHashcodeForTwoIdenticalAddresses() {
        assertEquals(firstAddress.hashCode(), secondAddress.hashCode());
    }

    @Test
    public void shouldComparingTwoSimilarAddressesReturnTrueIgnoringDifferenceBetweenNullOrEmptyString() {
        assertTrue(addressOne.equals(addressTwo));
    }

    @Test
    public void shouldBeSameHashcodeForTwoSimilarAddressesIgnoringDifferenceBetweenNullOrEmptyString() {
        assertEquals(addressOne.hashCode(), addressTwo.hashCode());
    }

    @Test
    public void shouldComparingTwoDifferentAddressesReturnFalse() {
        assertFalse(aAddress.equals(bAddress));
    }

    @Test
    public void shouldBeDifferentHashcodeForTwoDifferentAddresses() {
        assertNotEquals(aAddress.hashCode(), bAddress.hashCode());
    }

    @Test
    public void shouldBeSameHashcodeForTwoIdenticalStringArray() {
        assertEquals(DomainUtils.hashCode(stringArrayOne), DomainUtils.hashCode(stringArrayOne));
    }

    @Test
    public void shouldBeSameHashcodeForTwoSimilarAddressArraysIgnoringDifferenceBetweenNullOrEmptyString() {
        assertEquals(DomainUtils.hashCode(stringsArrayTwo), DomainUtils.hashCode(stringArrayThree));
    }

    @Test
    public void shouldBeSameHashcodeForTwoIdenticalObjectArray() {
        assertEquals(DomainUtils.hash(ObjectArrayOne), DomainUtils.hash(ObjectArrayOne));
    }

    @Test
    public void shouldBeSameHashcodeForTwoSimilarObjectArraysIgnoringDifferenceBetweenNullOrEmptyString() {
        assertEquals(DomainUtils.hash(ObjectArrayTwo), DomainUtils.hash(ObjectArrayThree));
    }

    @Test
    public void shouldReturnFalseIfFirstAddressIsNullAndSecondIsNotNull() {
        assertFalse(DomainUtils.equals(null, "non null value"));
    }

    @Test
    public void shouldReturnTrueIfBothAddressesAreNull() {
        assertTrue(DomainUtils.equals(null, null));
    }
}

