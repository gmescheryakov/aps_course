package tools;

import java.math.BigDecimal;

public class SourceReport {
  private final int number;
  private int generatedRequestCount;
  private int processedRequestCount;
  private int rejectedRequestCount;
  private BigDecimal requestServiceTime;
  private BigDecimal requestTimeInBuffer;

  public SourceReport(int number) {
    this.number = number;
    this.requestServiceTime = new BigDecimal(0);
    this.requestTimeInBuffer = new BigDecimal(0);
  }

  public synchronized int getNumber() {
    return number;
  }

  public synchronized int getGeneratedRequestCount() {
    return generatedRequestCount;
  }

  public synchronized int getProcessedRequestCount() {
    return processedRequestCount;
  }

  public synchronized int getRejectedRequestCount() {
    return rejectedRequestCount;
  }

  public synchronized BigDecimal getRequestServiceTime() {
    return requestServiceTime;
  }

  public synchronized BigDecimal getRequestTimeInBuffer() {
    return requestTimeInBuffer;
  }

  public synchronized void incrementGeneratedRequestCount() {
    ++generatedRequestCount;
  }

  public synchronized void incrementProcessedRequestCount() {
    ++processedRequestCount;
  }

  public synchronized void incrementRejectedRequestCount() {
    ++rejectedRequestCount;
  }

  public synchronized void addRequestServiceTime(long requestServiceTime) {
    this.requestServiceTime = this.requestServiceTime.add(BigDecimal.valueOf(requestServiceTime));
  }

  public synchronized void addRequestTimeInBuffer(long requestTimeInBuffer) {
    this.requestTimeInBuffer = this.requestTimeInBuffer.add(BigDecimal.valueOf(requestTimeInBuffer));
  }
}
