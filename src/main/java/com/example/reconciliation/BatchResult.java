package com.example.reconciliation;

import java.util.List;

public class BatchResult<T> {
  private final List<T> successfulRecords;
  private final List<ParseError> errors;
  private final int totalRowsProcessed;

  public BatchResult(List<T> successfulRecords, List<ParseError> errors, int totalRowsProcessed) {
    this.successfulRecords = successfulRecords;
    this.errors = errors;
    this.totalRowsProcessed = totalRowsProcessed;
  }

  public List<T> getSuccessfulRecords() { return successfulRecords; }
  public List<ParseError> getErrors() { return errors; }
  public int getTotalRowsProcessed() { return totalRowsProcessed; }
  public boolean hasErrors() { return !errors.isEmpty(); }
}
