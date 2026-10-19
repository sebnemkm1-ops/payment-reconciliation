package com.example.reconciliation;

public class ParseError {
  private final int lineNumber;
  private final String rawLine;
  private final String errorMessage;

  public ParseError(int lineNumber, String rawLine, String errorMessage) {
    this.lineNumber = lineNumber;
    this.rawLine = rawLine;
    this.errorMessage = errorMessage;
  }

  public int getLineNumber() { return lineNumber; }
  public String getRawLine() { return rawLine; }
  public String getErrorMessage() { return errorMessage; }
}
