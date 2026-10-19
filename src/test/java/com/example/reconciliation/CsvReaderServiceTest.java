package com.example.reconciliation;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Optional;
import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.*;

public class CsvReaderServiceTest {

  private final CsvParsingSystem.CsvReaderService readerService = new CsvParsingSystem.CsvReaderService();

  @Test
  public void testReadInvoicesSuccessfully() throws IOException {
    BatchResult<Invoice> result = (BatchResult<Invoice>) readerService.readInvoices(String.valueOf(Paths.get("invoices.csv")));    assertFalse(result.hasErrors(), "Invoices should parse without errors");
    assertEquals(10, result.getSuccessfulRecords().size());

    Invoice first = result.getSuccessfulRecords().get(0);
    assertEquals("INV-1001", first.getInvoiceId());
    assertTrue(first.hasSameAmount(new BigDecimal("1200.00")));
  }

  @Test
  public void testReadPaymentsWithQuotedAmountsAndRefunds() throws IOException {
    BatchResult<Payment> result = (BatchResult<Payment>) readerService.readPayments(String.valueOf(Paths.get("payments.csv")));

    assertFalse(result.hasErrors(), "Payments should parse without errors");
    assertEquals(11, result.getSuccessfulRecords().size());

    // Verify quoted amount parsing for P-10 ("3,400.00")
    Payment p10 = result.getSuccessfulRecords().stream()
      .filter(p -> p.getPaymentId().equals("P-10"))
      .findFirst()
      .orElseThrow();
    assertTrue(p10.hasSameAmount(new BigDecimal("3400.00")));

    // Verify negative refund parsing for P-11 (-75.25)
    Payment p11 = result.getSuccessfulRecords().stream()
      .filter(p -> p.getPaymentId().equals("P-11"))
      .findFirst()
      .orElseThrow();
    assertTrue(p11.isRefund());
    assertTrue(p11.hasSameAmount(new BigDecimal("-75.25")));
  }

  @Test
  public void testInvoiceTokenExtractionFromFreeTextReferences() {
    // Test "INV-1003 part 1" extracts "INV-1003"
    Payment p3 = new Payment("P-03", LocalDate.of(2026, 9, 3), new BigDecimal("500.00"), "GBP", "INV-1003 part 1");
    Optional<String> candidate3 = p3.getNormalizedInvoiceCandidate();
    assertTrue(candidate3.isPresent());
    assertEquals("INV-1003", candidate3.get());

    // Test lowercase inv1002 extracts "INV-1002"
    Payment p2 = new Payment("P-02", LocalDate.of(2026, 9, 1), new BigDecimal("450.00"), "GBP", "inv1002");
    Optional<String> candidate2 = p2.getNormalizedInvoiceCandidate();
    assertTrue(candidate2.isPresent());
    assertEquals("INV-1002", candidate2.get());

    // Test non-matching reference
    Payment p8 = new Payment("P-08", LocalDate.of(2026, 9, 15), new BigDecimal("150.00"), "GBP", "FABLE AND CO");
    assertFalse(p8.getNormalizedInvoiceCandidate().isPresent());
  }
}
