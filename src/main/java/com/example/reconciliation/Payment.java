package com.example.reconciliation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Payment {
  private final String paymentId;
  private final LocalDate paymentDate;
  private final BigDecimal amount;
  private final String currency;
  private final String reference;

  private static final Pattern INVOICE_TOKEN_PATTERN = Pattern.compile("INV-?\\d+", Pattern.CASE_INSENSITIVE);

  public Payment(String paymentId, LocalDate paymentDate, BigDecimal amount, String currency, String reference) {
    this.paymentId = paymentId;
    this.paymentDate = paymentDate;
    this.amount = amount;
    this.currency = currency;
    this.reference = reference;
  }

  public String getPaymentId() { return paymentId; }
  public LocalDate getPaymentDate() { return paymentDate; }
  public BigDecimal getAmount() { return amount; }
  public String getCurrency() { return currency; }
  public String getReference() { return reference; }

  public boolean isRefund() {
    return amount.signum() < 0 || (reference != null && reference.toUpperCase().contains("REFUND"));
  }

  public Optional<String> getNormalizedInvoiceCandidate() {
    if (reference == null || reference.trim().isEmpty()) {
      return Optional.empty();
    }
    Matcher matcher = INVOICE_TOKEN_PATTERN.matcher(reference);
    if (matcher.find()) {
      String token = matcher.group(0).toUpperCase();
      if (!token.contains("-")) {
        token = token.replace("INV", "INV-");
      }
      return Optional.of(token);
    }
    return Optional.empty();
  }

  public boolean hasSameAmount(BigDecimal otherAmount) {
    if (otherAmount == null) return false;
    return this.amount.compareTo(otherAmount) == 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Payment payment = (Payment) o;
    return Objects.equals(paymentId, payment.paymentId) &&
      Objects.equals(paymentDate, payment.paymentDate) &&
      amount.compareTo(payment.amount) == 0 &&
      Objects.equals(currency, payment.currency) &&
      Objects.equals(reference, payment.reference);
  }

  @Override
  public int hashCode() {
    BigDecimal normalizedAmount = (amount != null) ? amount.stripTrailingZeros() : null;
    return Objects.hash(paymentId, paymentDate, normalizedAmount, currency, reference);
  }
}
