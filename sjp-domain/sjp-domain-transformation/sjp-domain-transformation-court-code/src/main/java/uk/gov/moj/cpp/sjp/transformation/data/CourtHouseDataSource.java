package uk.gov.moj.cpp.sjp.transformation.data;

/**
 * Defines a mechanism for finding court codes by name.
 */
public interface CourtHouseDataSource {

    /**
     * Finds the court code for a given court name.
     *
     * @param courtName the court name
     * @return the court code
     */
    String getCourtCodeForName(String courtName);

    /**
     * Creates an {@link InMemoryCourtHouseDataSource} instance.
     *
     * @return the instance
     */
    static CourtHouseDataSource inMemory() {
        return InMemoryCourtHouseDataSource.INSTANCE;
    }

}
