Java Invoice & Payment CSV Parsing System

A robust, production-grade Java system for reading, parsing, validating, and reconciling invoices.csv and payments.csv files. Built with strong domain models, strict precision for financial data using BigDecimal, resilient error tracking, and automated JUnit 5 tests.

🌟 Key Features

Strict Financial Precision (BigDecimal): Avoids floating-point rounding errors by handling all monetary amounts with BigDecimal.

Scale-Insensitive Equality & HashCode: Uses .compareTo() == 0 for monetary evaluations and normalizes amounts via .stripTrailingZeros() in hashCode() to maintain strict consistency between .equals() and .hashCode().

Regex-Based Invoice Token Extraction: Uses regular expressions to extract structured invoice ID candidates (e.g., extracting "INV-1003" from complex free-text references like "INV-1003 part 1" or "inv1002").

Complete Input Row Accounting: No rows are silently dropped. Parsing results return a BatchResult<T> containing successfully parsed records, total row counts, and a detailed list of ParseError objects capturing line numbers, raw CSV text, and error causes.

Strict CSV Header Validation: Ensures incoming CSV files adhere to expected column structures before parsing records.

Quoted Amounts & Refund Support: Correctly handles quoted fields (e.g., "3,400.00"), strips comma separators, and accepts negative values (e.g., "-75.25") while flagging payment refunds.

📋 Requirements

Java Development Kit (JDK): Version 8 or higher (Java 8+).

Test Framework: JUnit 5 (Jupiter).

Build Tool (Optional): Maven or Gradle.

📁 Directory Structure

├── input/
│   ├── invoices.csv          # Input invoice data
│   └── payments.csv          # Input payment data
├── src/
│   ├── main/
│   │   └── java/
│   │       ├── BatchResult.java
│   │       ├── CsvReaderService.java
│   │       ├── Invoice.java
│   │       ├── ParseError.java
│   │       └── Payment.java
│   └── test/
│       └── java/
│           └── CsvReaderServiceTest.java
└── README.md


🚀 Usage Examples

Reading and Processing Invoices & Payments

import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) throws Exception {
        CsvReaderService readerService = new CsvReaderService();

        // 1. Parse Invoices
        Path invoicePath = Paths.get("input/invoices.csv");
        BatchResult<Invoice> invoiceBatch = readerService.readInvoices(invoicePath);

        System.out.println("Total Rows Processed: " + invoiceBatch.getTotalRowsProcessed());
        System.out.println("Successful Invoices: " + invoiceBatch.getSuccessfulRecords().size());

        // Handle Errors if any
        if (invoiceBatch.hasErrors()) {
            for (ParseError error : invoiceBatch.getErrors()) {
                System.err.println("Error at line " + error.getLineNumber() + ": " + error.getErrorMessage());
            }
        }

        // 2. Parse Payments
        Path paymentPath = Paths.get("input/payments.csv");
        BatchResult<Payment> paymentBatch = readerService.readPayments(paymentPath);

        for (Payment payment : paymentBatch.getSuccessfulRecords()) {
            // Check for refunds
            if (payment.isRefund()) {
                System.out.println("Refund Detected [" + payment.getPaymentId() + "]: " + payment.getAmount());
            }

            // Extract potential invoice candidate from free-text reference
            payment.getNormalizedInvoiceCandidate().ifPresent(candidate -> 
                System.out.println("Extracted Candidate: " + candidate + " for Payment: " + payment.getPaymentId())
            );
        }
    }
}


🧪 Running the Tests

Tests are written using JUnit 5.

Using Maven

To execute the test suite via Maven, run:

mvn test


Using Gradle

To execute the test suite via Gradle, run:

gradle test


Running in IDE (IntelliJ / Eclipse / VS Code)

Open src/test/java/CsvReaderServiceTest.java.

Right-click the class name or individual @Test methods.

Select Run 'CsvReaderServiceTest'.

📊 CSV File Formats

invoices.csv

invoice_id,customer,amount,currency,issued,due
INV-1001,ACME Corp,"1,200.00",USD,2026-01-15,2026-02-15
INV-1002,Stark Ind,500.00,EUR,2026-01-16,2026-02-16


payments.csv

payment_id,date,amount,currency,reference
P-10,2026-01-20,"3,400.00",USD,INV-1001
P-11,2026-01-22,-75.25,EUR,Partial refund for INV-1002
