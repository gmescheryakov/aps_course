package source;

import bufferAndManagers.BufferManager;
import tools.Report;

import javax.swing.table.DefaultTableModel;

import static tools.ConstantsAndParameters.*;

public class Source implements Runnable {
  private static int count;

  private final int number;
  private final BufferManager bufferManager;
  private final Report report;
  public static boolean isTrue = true;
  private DefaultTableModel modelStepSource;

  private final Object stepReportSynchronizer;

  public Source(BufferManager bufferManager, Report report, Object stepReportSynchronizer, DefaultTableModel modelStepSource) {
    this.number = count++;
    this.bufferManager = bufferManager;
    this.report = report;
    this.stepReportSynchronizer = stepReportSynchronizer;
    this.modelStepSource = modelStepSource;
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

      if (isTrue) {
        modelStepSource.addColumn("generation");
        isTrue = false;
      }
//      modelStepBuf.addRow(number);
      Object[] object = new Object[1];
      object[0] = "Source " + number + ": generate request " + request.getNumber();
      modelStepSource.addRow(object);
//      System.out.println("Source " + number + ": generate request " + request.getNumber());
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

  public static void resetCounter(){
    count = 0;
  }
}
