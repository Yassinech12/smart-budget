package com.yassinecharit.budget.transaction;

import com.yassinecharit.budget.common.BadRequestException;
import com.yassinecharit.budget.transaction.CsvTransactionParser.ParseResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvTransactionParserTest {

    private final CsvTransactionParser parser = new CsvTransactionParser();

    private ParseResult parse(String csv) {
        return parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesFrenchBankFormat() {
        ParseResult result = parse("date;libellé;montant\n02/10/2026;PRLV LOYER;-1 234,56\n");
        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.date()).isEqualTo(LocalDate.of(2026, 10, 2));
            assertThat(row.label()).isEqualTo("PRLV LOYER");
            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("-1234.56"));
        });
    }

    @Test
    void parsesIsoFormatWithCommaSeparatorAndQuotes() {
        ParseResult result = parse("label,amount,date\n\"CB AMAZON, PARIS\",-34.99,2026-10-14\n");
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.label()).isEqualTo("CB AMAZON, PARIS");
            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("-34.99"));
            assertThat(row.date()).isEqualTo(LocalDate.of(2026, 10, 14));
        });
    }

    @Test
    void reportsInvalidLinesWithoutStopping() {
        ParseResult result = parse("date;label;amount\n31/13/2026;BAD DATE;-10\n01/10/2026;OK;-5,00\n");
        assertThat(result.rows()).hasSize(1);
        assertThat(result.errors()).singleElement().asString().startsWith("Line 2");
    }

    @Test
    void rejectsAFileWithoutTheExpectedColumns() {
        assertThatThrownBy(() -> parse("foo;bar\n1;2\n")).isInstanceOf(BadRequestException.class);
    }
}
