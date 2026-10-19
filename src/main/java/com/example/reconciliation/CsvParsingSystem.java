package com.example.reconciliation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

public class CsvParsingSystem {

  // ==========================================
  // 1. DOMAIN MODELS
  // ==========================================

  public static class Invoice {
    private final String invoiceId;
    private final String customerId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final BigDecimal amount;
    private final String currency;

    public Invoice(String invoiceId, String customerId, LocalDate issueDate, LocalDate dueDate, BigDecimal amount, String currency) {
      this.invoiceId = invoiceId;
      this.customerId = customerId;
      this.issueDate = issueDate;
      this.dueDate = dueDate;
      this.amount = amount;
      this.currency = currency;
    }

    public String getInvoiceId() { return invoiceId; }
    public String getCustomerId() { return customerId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    /**
     * Scale-insensitive comparison for monetary amounts.
     */
    public boolean hasSameAmount(BigDecimal otherAmount) {
      if (otherAmount == null || this.amount == null) return false;
      return this.amount.compareTo(otherAmount) == 0;
    }

    @Override
    public String toString() {
      return "Invoice{" +
        "invoiceId='" + invoiceId + '\'' +
        ", customerId='" + customerId + '\'' +
        ", issueDate=" + issueDate +
        ", dueDate=" + dueDate +
        ", amount=" + amount +
        ", currency='" + currency + '\'' +
        '}';
    }
  }

  public static class Payment {
    private final String paymentId;
    private final String reference; // Raw free-text reference field
    private final LocalDate paymentDate;
    private final BigDecimal amount;
    private final String currency;

    public Payment(String paymentId, String reference, LocalDate paymentDate, BigDecimal amount, String currency) {
      this.paymentId = paymentId;
      this.reference = reference;
      this.paymentDate = paymentDate;
      this.amount = amount;
      this.currency = currency;
    }

    public String getPaymentId() { return paymentId; }
    public String getReference() { return reference; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    /**
     * Checks if the payment represents a refund (negative amount or explicit refund term in reference).
     */
    public boolean isRefund() {
      boolean negativeAmount = amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
      boolean explicitRefundText = reference != null && reference.toUpperCase().contains("REFUND");
      return negativeAmount || explicitRefundText;
    }

    /**
     * Derives a normalized candidate invoice ID separately from the raw reference string.
     */
    public Optional<String> getNormalizedInvoiceCandidate() {
      if (reference == null || reference.trim().isEmpty()) {
        return Optional.empty();
      }

      String upper = reference.trim().toUpperCase();

      // Filter out non-invoice references
      if (upper.contains("REFUND") || upper.contains("CASH") || upper.contains("PAYPAL")) {
        return Optional.empty();
      }

      // Remove common prefixes if present, e.g., "INV-1001" -> "1001" or keep full standard ID
      String cleaned = reference.trim().replaceAll("[^a-zA-Z0-9-]", "");
      return cleaned.isEmpty() ? Optional.empty() : Optional.of(cleaned);
    }

    /**
     * Scale-insensitive comparison for monetary amounts.
     */
    public boolean hasSameAmount(BigDecimal otherAmount) {
      if (otherAmount == null || this.amount == null) return false;
      return this.amount.compareTo(otherAmount) == 0;
    }

    @Override
    public String toString() {
      return "Payment{" +
        "paymentId='" + paymentId + '\'' +
        ", reference='" + reference + '\'' +
        ", paymentDate=" + paymentDate +
        ", amount=" + amount +
        ", currency='" + currency + '\'' +
        '}';
    }
  }

  // ==========================================
  // 2. ROBUST CSV PARSER SERVICE
  // ==========================================

  public static class CsvReaderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Custom CSV line tokenizer capable of handling quotes, commas within quoted strings,
     * and escaped quotes ("").
     */
    public static List<String> parseCsvLine(String line) {
      List<String> tokens = new ArrayList<>();
      if (line == null) return tokens;

      StringBuilder current = new StringBuilder();
      boolean inQuotes = false;

      for (int i = 0; i < line.length(); i++) {
        char c = line.charAt(i);

        if (c == '"') {
          // Check for escaped quotes ("")
          if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++; // Skip second quote
          } else {
            inQuotes = !inQuotes; // Toggle quote state
          }
        } else if (c == ',' && !inQuotes) {
          tokens.add(current.toString().trim());
          current.setLength(0);
        } else {
          current.append(c);
        }
      }
      tokens.add(current.toString().trim());
      return tokens;
    }

    public List<Invoice> readInvoices(String csvContent) throws IOException {
      List<Invoice> invoices = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
        String line;
        boolean isHeader = true;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
          lineNumber++;
          if (isHeader) {
            isHeader = false;
            continue;
          }
          if (line.trim().isEmpty()) continue;

          List<String> tokens = parseCsvLine(line);
          if (tokens.size() < 6) {
            throw new IllegalArgumentException("Insufficient columns at line " + lineNumber);
          }

          try {
            String invoiceId = validateRequired(tokens.get(0), "invoiceId", lineNumber);
            String customerId = validateRequired(tokens.get(1), "customerId", lineNumber);
            LocalDate issueDate = parseDate(tokens.get(2), "issueDate", lineNumber);
            LocalDate dueDate = parseDate(tokens.get(3), "dueDate", lineNumber);
            BigDecimal amount = parseBigDecimal(tokens.get(4), "amount", lineNumber, false);
            String currency = validateCurrency(tokens.get(5), lineNumber);

            invoices.add(new Invoice(invoiceId, customerId, issueDate, dueDate, amount, currency));
          } catch (Exception e) {
            System.err.println("Error parsing invoice at line " + lineNumber + ": " + e.getMessage());
          }
        }
      }
      return invoices;
    }

    public List<Payment> readPayments(String csvContent) throws IOException {
      List<Payment> payments = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
        String line;
        boolean isHeader = true;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
          lineNumber++;
          if (isHeader) {
            isHeader = false;
            continue;
          }
          if (line.trim().isEmpty()) continue;

          List<String> tokens = parseCsvLine(line);
          if (tokens.size() < 5) {
            throw new IllegalArgumentException("Insufficient columns at line " + lineNumber);
          }

          try {
            String paymentId = validateRequired(tokens.get(0), "paymentId", lineNumber);
            String reference = tokens.get(1); // Free-text reference, can be empty
            LocalDate paymentDate = parseDate(tokens.get(2), "paymentDate", lineNumber);
            BigDecimal amount = parseBigDecimal(tokens.get(3), "amount", lineNumber, true); // Allows negative for refunds
            String currency = validateCurrency(tokens.get(4), lineNumber);

            payments.add(new Payment(paymentId, reference, paymentDate, amount, currency));
          } catch (Exception e) {
            System.err.println("Error parsing payment at line " + lineNumber + ": " + e.getMessage());
          }
        }
      }
      return payments;
    }

    private String validateRequired(String value, String fieldName, int lineNumber) {
      if (value == null || value.trim().isEmpty()) {
        throw new IllegalArgumentException("Missing required field '" + fieldName + "' at line " + lineNumber);
      }
      return value.trim();
    }

    private static final List<DateTimeFormatter> ACCEPTED_DATE_FORMATTERS = Arrays.asList(
      DateTimeFormatter.ISO_LOCAL_DATE,             // 2026-03-01
      DateTimeFormatter.ofPattern("M/d/yyyy"),        // 3/1/2026 or 03/01/2026
      DateTimeFormatter.ofPattern("dd-MM-yyyy")       // 01-03-2026
    );

    private LocalDate parseDate(String value, String fieldName, int lineNumber) {
      String cleanedValue = validateRequired(value, fieldName, lineNumber);

      // 1. Strip surrounding quotes if the CSV field was quoted (e.g., "2026-03-01" -> 2026-03-01)
      cleanedValue = cleanedValue.replaceAll("^\"+|\"+$", "").trim();

      // 2. Try parsing against accepted date formats
      for (DateTimeFormatter formatter : ACCEPTED_DATE_FORMATTERS) {
        try {
          return LocalDate.parse(cleanedValue, formatter);
        } catch (DateTimeParseException e) {
          // Try next format
        }
      }

      // If none match, throw a clear error showing what was received
      throw new IllegalArgumentException(
        "Invalid date format for '" + fieldName + "' ('" + value + "') at line " + lineNumber +
          ". Expected format: YYYY-MM-DD."
      );
    }
    private BigDecimal parseBigDecimal(String value, String fieldName, int lineNumber, boolean allowNegative) {
      validateRequired(value, fieldName, lineNumber);
      // Remove currency symbols, extra spaces, or thousands separators if needed
      String cleanValue = value.replaceAll("[$,]", "").trim();
      try {
        BigDecimal amount = new BigDecimal(cleanValue);
        if (!allowNegative && amount.signum() < 0) {
          throw new IllegalArgumentException("Negative amount not allowed for '" + fieldName + "' at line " + lineNumber);
        }
        return amount;
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid monetary value '" + value + "' at line " + lineNumber);
      }
    }

    private String validateCurrency(String value, int lineNumber) {
      String currency = validateRequired(value, "currency", lineNumber);
      if (!currency.matches("^[A-Z]{3}$")) {
        throw new IllegalArgumentException("Invalid ISO currency code ('" + currency + "') at line " + lineNumber);
      }
      return currency;
    }
  }

  // ==========================================
  // 3. MAIN TESTING ENVIRONMENT
  // ==========================================

  public static void main(String[] args) throws Exception {
    System.out.println("Starting CSV Parsing and BigDecimal Tests...\n");

    CsvReaderService parser = new CsvReaderService();

    // Sample CSV with quoted amounts, negative refund amounts, and free-text references
    String invoiceCsv = "invoiceId,customerId,issueDate,dueDate,amount,currency\n" +
      "INV-1001,CUST-01,2026-01-15,2026-02-15,\"1,250.50\",USD\n" +
      "INV-1002,CUST-02,2026-01-16,2026-02-16,500.00,EUR";

    String paymentCsv = "paymentId,reference,paymentDate,amount,currency\n" +
      "PAY-8001,\"INV-1001\",2026-01-20,\"1250.5000\",USD\n" +
      "PAY-8002,\"Partial refund for INV-1002\",2026-01-22,\"-150.00\",EUR\n" +
      "PAY-8003,,2026-01-25,50.00,USD";

    // Parse Invoices
    List<Invoice> invoices = parser.readInvoices(invoiceCsv);
    System.out.println("--- Parsed Invoices ---");
    invoices.forEach(System.out::println);

    // Parse Payments
    List<Payment> payments = parser.readPayments(paymentCsv);
    System.out.println("\n--- Parsed Payments ---");
    payments.forEach(System.out::println);

    // Verify Tests
    System.out.println("\n--- Test Verification ---");

    // Test 1: Quoted Amount Parsing & Scale-Insensitive Comparison
    Invoice inv1 = invoices.get(0);
    Payment pay1 = payments.get(0);
    boolean scaleTest = inv1.hasSameAmount(pay1.getAmount());
    System.out.println("Test 1 [BigDecimal compareTo Scale Check]: " +
      (scaleTest ? "PASSED (1250.50 equals 1250.5000)" : "FAILED"));

    // Test 2: Negative/Refund Amount
    Payment refundPayment = payments.get(1);
    boolean refundTest = refundPayment.isRefund() && refundPayment.getAmount().compareTo(BigDecimal.ZERO) < 0;
    System.out.println("Test 2 [Negative Refund Handling]: " +
      (refundTest ? "PASSED (Detected -150.00 refund)" : "FAILED"));

    // Test 3: Free-text Reference & Candidate Derivation
    Payment freeTextPayment = payments.get(1);
    Optional<String> candidate = freeTextPayment.getNormalizedInvoiceCandidate();
    System.out.println("Test 3 [Free-text Normalized Candidate]: " +
      (candidate.isEmpty() ? "PASSED (Filtered out text containing 'refund')" : "FAILED"));

    Payment structuredPayment = payments.get(0);
    Optional<String> structCandidate = structuredPayment.getNormalizedInvoiceCandidate();
    System.out.println("Test 4 [Structured Reference Normalization]: " +
      (structCandidate.isPresent() && structCandidate.get().equals("INV-1001") ? "PASSED (Extracted INV-1001)" : "FAILED"));
  }
}
