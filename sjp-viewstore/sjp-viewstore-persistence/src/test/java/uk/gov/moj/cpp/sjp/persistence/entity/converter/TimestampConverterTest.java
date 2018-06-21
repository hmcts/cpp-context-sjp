package uk.gov.moj.cpp.sjp.persistence.entity.converter;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

import org.junit.Test;

public class TimestampConverterTest {

    private final static ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.now(UTC);
    private final static Timestamp TIMESTAMP = Timestamp.from(ZONED_DATE_TIME.toInstant());

    private TimestampConverter underTest = new TimestampConverter();

    @Test
    public void convertToDatabaseColumnIsNullable() {
        assertThat(underTest.convertToDatabaseColumn(null), nullValue());
    }

    @Test
    public void convertToDatabaseColumn() {
        assertThat(underTest.convertToDatabaseColumn(ZONED_DATE_TIME), equalTo(TIMESTAMP));
    }

    @Test
    public void convertToEntityAttributeIsNullable() {
        assertThat(underTest.convertToEntityAttribute(null), nullValue());
    }

    @Test
    public void convertToEntityAttribute() {
        assertThat(underTest.convertToEntityAttribute(TIMESTAMP), equalTo(ZONED_DATE_TIME));
    }

}