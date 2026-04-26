package com.jpablodrexler.photomanager.infrastructure.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConverterTest {

    @InjectMocks
    LocalDateTimeConverter sut;

    @Test
    void convertToDatabaseColumn_nullValue_returnsNull() {
        String result = sut.convertToDatabaseColumn(null);

        assertThat(result).isNull();
    }

    @Test
    void convertToDatabaseColumn_validDateTime_returnsISOFormattedString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45);

        String result = sut.convertToDatabaseColumn(dateTime);

        assertThat(result).isEqualTo("2024-06-15T10:30:45");
    }

    @Test
    void convertToDatabaseColumn_dateTimeWithNanos_preservesNanos() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45, 123456789);

        String result = sut.convertToDatabaseColumn(dateTime);

        assertThat(result).startsWith("2024-06-15T10:30:45");
        assertThat(result).contains("123456789");
    }

    @Test
    void convertToEntityAttribute_nullValue_returnsNull() {
        LocalDateTime result = sut.convertToEntityAttribute(null);

        assertThat(result).isNull();
    }

    @Test
    void convertToEntityAttribute_emptyString_returnsNull() {
        LocalDateTime result = sut.convertToEntityAttribute("");

        assertThat(result).isNull();
    }

    @Test
    void convertToEntityAttribute_blankString_returnsNull() {
        LocalDateTime result = sut.convertToEntityAttribute("   ");

        assertThat(result).isNull();
    }

    @Test
    void convertToEntityAttribute_validISOString_returnsLocalDateTime() {
        LocalDateTime result = sut.convertToEntityAttribute("2024-06-15T10:30:45");

        assertThat(result).isEqualTo(LocalDateTime.of(2024, 6, 15, 10, 30, 45));
    }

    @Test
    void convertToEntityAttribute_invalidString_returnsNull() {
        LocalDateTime result = sut.convertToEntityAttribute("not-a-date");

        assertThat(result).isNull();
    }

    @Test
    void convertToDatabaseColumn_roundTrip_preservesValue() {
        LocalDateTime original = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        String dbColumn = sut.convertToDatabaseColumn(original);
        LocalDateTime restored = sut.convertToEntityAttribute(dbColumn);

        assertThat(restored).isEqualTo(original);
    }
}
