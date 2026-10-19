package com.example.reconciliation;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CsvReaderService {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final String CSV_SPLIT_REGEX = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

  public BatchResult<Invoice> readInvoices(Path filePath) throws IOException {
    List<Invoice> invoices = new ArrayList<>();
    List<ParseError> errors = new ArrayList<>();
    int lineNumber = 0;

    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
      String headerLine = reader.readLine();
      lineNumber++;
      if (headerLine != null) {
        validateHeader(headerLine, "invoice_id,customer,amount,currency,issued,due", lineNumber);
      }

      String line;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.trim().isEmpty()) continue;

        String[] tokens = splitCsvLine(line);
        if (tokens.length < 6) {
          errors.add(new ParseError(lineNumber, line, "Insufficient columns (expected 6)"));
          continue;
        }

        try {
          String invoiceId = validateRequired(tokens[0], "invoice_id", lineNumber);
          String customer = validateRequired(tokens[1], "customer", lineNumber);
          BigDecimal amount = parseBigDecimal(tokens[2], "amount", lineNumber, false);
          String currency = validateCurrency(tokens[3], lineNumber);
          LocalDate issued = parseDate(tokens[4], "issued", lineNumber);
          LocalDate due = parseDate(tokens[5], "due", lineNumber);

          invoices.add(new Invoice(invoiceId, customer, amount, currency, issued, due));
        } catch (Exception e) {
          errors.add(new ParseError(lineNumber, line, e.getMessage()));
        }
      }
    }
    return new BatchResult<>(invoices, errors, lineNumber - 1);
  }

  public BatchResult<Payment> readPayments(Path filePath) throws IOException {
    List<Payment> payments = new ArrayList<>();
    List<ParseError> errors = new ArrayList<>();
    int lineNumber = 0;

    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
      String headerLine = reader.readLine();
      lineNumber++;
      if (headerLine != null) {
        validateHeader(headerLine, "payment_id,date,amount,currency,reference", lineNumber);
      }

      String line;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.trim().isEmpty()) continue;

        String[] tokens = splitCsvLine(line);
        if (tokens.length < 5) {
          errors.add(new ParseError(lineNumber, line, "Insufficient columns (expected 5)"));
          continue;
        }

        try {
          String paymentId = validateRequired(tokens[0], "payment_id", lineNumber);
          LocalDate date = parseDate(tokens[1], "date", lineNumber);
          BigDecimal amount = parseBigDecimal(tokens[2], "amount", lineNumber, true); // allowNegative = true for refunds
          String currency = validateCurrency(tokens[3], lineNumber);
          String reference = tokens[4].trim();

          payments.add(new Payment(paymentId, date, amount, currency, reference));
        } catch (Exception e) {
          errors.add(new ParseError(lineNumber, line, e.getMessage()));
        }
      }
    }
    return new BatchResult<>(payments, errors, lineNumber - 1);
  }

  private void validateHeader(String actualHeader, String expectedHeader, int lineNumber) {
    String cleanedActual = actualHeader.trim().replaceAll("\\s+", "");
    String cleanedExpected = expectedHeader.trim().replaceAll("\\s+", "");
    if (!cleanedActual.equalsIgnoreCase(cleanedExpected)) {
      throw new IllegalArgumentException("Header mismatch at line " + lineNumber);
    }
  }

  private String[] splitCsvLine(String line) {
    String[] rawTokens = line.split(CSV_SPLIT_REGEX, -1);
    for (int i = 0; i < rawTokens.length; i++) {
      rawTokens[i] = rawTokens[i].trim().replaceAll("^\"+|\"+$", "");
    }
    return rawTokens;
  }

  private String validateRequired(String value, String fieldName, int lineNumber) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Missing required field '" + fieldName + "'");
    }
    return value;
  }

  private LocalDate parseDate(String value, String fieldName, int lineNumber) {
    String cleaned = validateRequired(value, fieldName, lineNumber);
    try {
      return LocalDate.parse(cleaned, DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date format for '" + fieldName + "'");
    }
  }

  private BigDecimal parseBigDecimal(String value, String fieldName, int lineNumber, boolean allowNegative) {
    String cleaned = validateRequired(value, fieldName, lineNumber).replace(",", "");
    try {
      BigDecimal amount = new BigDecimal(cleaned);
      if (!allowNegative && amount.signum() < 0) {
        throw new IllegalArgumentException("Amount cannot be negative for '" + fieldName + "'");
      }
      return amount;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid numeric format for '" + fieldName + "'");
    }
  }

  private String validateCurrency(String value, int lineNumber) {
    String currency = validateRequired(value, "currency", lineNumber);
    if (!currency.matches("^[A-Z]{3}$")) {
      throw new IllegalArgumentException("Invalid currency code");
    }
    return currency;
  }
}
