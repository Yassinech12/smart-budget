package com.yassinecharit.budget.transaction;

import com.yassinecharit.budget.common.BadRequestException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses a bank statement exported as CSV.
 * Expected header: date;label;amount (separator ";" or ",").
 * Accepted formats: dates "31/10/2026" or "2026-10-31", amounts "-1 234,56" or "-1234.56".
 */
public class CsvTransactionParser {

    private static final DateTimeFormatter FRENCH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record ParsedRow(LocalDate date, String label, BigDecimal amount) {
    }

    public record ParseResult(List<ParsedRow> rows, List<String> errors) {
    }

    public ParseResult parse(InputStream input) {
        List<ParsedRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new BadRequestException("The CSV file is empty");
            }
            header = header.replace("﻿", ""); // Excel BOM
            char separator = header.contains(";") ? ';' : ',';
            int[] columns = findColumns(splitLine(header, separator));

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    List<String> cells = splitLine(line, separator);
                    rows.add(new ParsedRow(parseDate(cells.get(columns[0])), cells.get(columns[1]).trim(),
                            parseAmount(cells.get(columns[2]))));
                } catch (RuntimeException e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Unable to read the CSV file");
        }
        return new ParseResult(rows, errors);
    }

    private int[] findColumns(List<String> header) {
        int date = -1, label = -1, amount = -1;
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i).trim().toLowerCase(Locale.ROOT);
            if (h.equals("date")) date = i;
            else if (h.equals("label") || h.equals("libelle") || h.equals("libellé")) label = i;
            else if (h.equals("amount") || h.equals("montant")) amount = i;
        }
        if (date < 0 || label < 0 || amount < 0) {
            throw new BadRequestException("The CSV header must contain the columns date, label and amount");
        }
        return new int[]{date, label, amount};
    }

    /** Splits a CSV line, respecting double quotes. */
    static List<String> splitLine(String line, char separator) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == separator && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    static LocalDate parseDate(String value) {
        String v = value.trim();
        try {
            return v.contains("/") ? LocalDate.parse(v, FRENCH_DATE) : LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("invalid date '" + v + "'");
        }
    }

    static BigDecimal parseAmount(String value) {
        String v = value.trim().replace(" ", "").replace(" ", "").replace("€", "");
        if (v.contains(",")) {
            v = v.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid amount '" + value.trim() + "'");
        }
    }
}
