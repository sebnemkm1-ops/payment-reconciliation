package com.example.reconciliation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Invoice {
  private final String invoiceId;
  private final String customer;
  private final BigDecimal amount;
  private final String currency;
  private final LocalDate issueDate;
  private final LocalDate dueDate;

  public Invoice(String invoiceId, String customer, BigDecimal amount, String currency, LocalDate issueDate, LocalDate dueDate) {
    this.invoiceId = invoiceId;
    this.customer = customer;
    this.amount = amount;
    this.currency = currency;
    this.issueDate = issueDate;
    this.dueDate = dueDate;
  }

  public String getInvoiceId() { return invoiceId; }
  public String getCustomer() { return customer; }
  public BigDecimal getAmount() { return amount; }
  public String getCurrency() { return currency; }
  public LocalDate getIssueDate() { return issueDate; }
  public LocalDate getDueDate() { return dueDate; }

  public boolean hasSameAmount(BigDecimal otherAmount) {
    if (otherAmount == null) return false;
    return this.amount.compareTo(otherAmount) == 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Invoice invoice = (Invoice) o;
    return Objects.equals(invoiceId, invoice.invoiceId) &&
      Objects.equals(customer, invoice.customer) &&
      amount.compareTo(invoice.amount) == 0 &&
      Objects.equals(currency, invoice.currency) &&
      Objects.equals(issueDate, invoice.issueDate) &&
      Objects.equals(dueDate, invoice.dueDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(invoiceId, customer, amount, currency, issueDate, dueDate);
  }
}
