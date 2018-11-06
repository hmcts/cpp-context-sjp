package uk.gov.moj.cpp.sjp.transformation.data;

import static java.text.MessageFormat.format;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

/**
 * Defines an in-memory dictionary {@link CourtHouseDataSource} implementation.
 */
public class InMemoryCourtHouseDataSource implements CourtHouseDataSource {

    static final InMemoryCourtHouseDataSource INSTANCE = new InMemoryCourtHouseDataSource();

    private static final Map<String, String> COURT_NAME_TO_CODE = ImmutableMap.<String, String>builder()
            .put("Lavender Hill Magistrates' Court", "B01LY")
            .put("Barkingside Magistrates' Court", "B01KR")
            .put("Bexley Magistrates' Court' Court", "B01BH")
            .put("Bromley Magistrates' Court", "B01CN")
            .put("Camberwell Green Magistrates' Court", "B01CX")
            .put("City of London Magistrates' Court", "B01DU")
            .put("Coventry Magistrates' Court", "B20EB")
            .put("Croydon Magistrates' Court", "B01EF")
            .put("Ealing Magistrates' Court", "B01FA")
            .put("Hendon Magistrates' Court", "B01GQ")
            .put("Highbury Corner Magistrates' Court", "B01GU")
            .put("Leamington Spa Magistrates' Court", "B23HS")
            .put("Nuneaton Magistrates' Court", "B23PP")
            .put("Romford Magistrates' Court", "B01LA")
            .put("Stratford Magistrates' Court", "B01MN")
            .put("Thames Magistrates' Court", "B01ND")
            .put("Uxbridge Magistrates' Court", "B01NM")
            .put("Westminster Magistrates' Court", "B01IX")
            .put("Willesden Magistrates' Court", "B01CE")
            .put("Wimbledon Magistrates' Court", "B01OK")
            .put("Battersea", "B01LY")
            .build();

    private InMemoryCourtHouseDataSource() {
    }

    @Override
    public String getCourtCodeForName(String courtName) {
        return Optional.ofNullable(COURT_NAME_TO_CODE.get(courtName))
                .orElseThrow(() -> new IllegalArgumentException(format("Unknown court name {0}", courtName)));
    }

}
