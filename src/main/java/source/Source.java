package source;

import bufferAndManagers.BufferManager;
import tools.Report;

import static tools.ConstantsAndParameters.*;

public class Source implements Runnable {
  private static int count;

  private final int number;
  private final BufferManager bufferManager;
  private final Report report;

  private final Object stepReportSynchronizer;

  public Source(BufferManager bufferManager, Report report, Object stepReportSynchronizer) {
    this.number = count++;
    this.bufferManager = bufferManager;
    this.report = report;
    this.stepReportSynchronizer = stepReportSynchronizer;
  }

  public int getNumber() {
    return number;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      synchronized (stepReportSynchronizer) {
        try {
          stepReportSynchronizer.wait();
        } catch (InterruptedException ignored) {
          break;
        }
      }
      Request request = new Request(number);
      System.out.println("Source " + number + ": generate request " + request.getNumber());
      bufferManager.emplace(request);

      report.incrementGeneratedRequestCount(number);

      try {
        Thread.sleep((long) (Math.log(Math.random()) / (-LAMBDA)) * MILLISECONDS_PER_SECOND);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
}
