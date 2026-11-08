package com.yassinecharit.budget.common;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Parses the "month" query parameter (format yyyy-MM), defaulting to the current month. */
public final class MonthParam {

    private MonthParam() {
    }

    public static YearMonth parse(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("month must use the format yyyy-MM, for example 2026-10");
        }
    }
}
