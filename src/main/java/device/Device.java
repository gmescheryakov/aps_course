package device;

import bufferAndManagers.Buffer;
import source.Request;
import tools.Report;

import java.util.Queue;

import static tools.ConstantsAndParameters.*;

public class Device implements Runnable {
  private static int count;

  private final int number;
  private volatile Queue<Request> requestsPackage;
  private volatile boolean isFree;
  private volatile int packageNumber;
  private final Object newPackageNotifier;
  private final Report report;
  private final Object stepReportSynchronizer;

  private final Buffer buffer;

  public Device(Report report, Object stepReportSynchronizer, Buffer buffer) {
    this.number = count++;
    this.report = report;
    this.isFree = true;
    this.newPackageNotifier = new Object();
    this.stepReportSynchronizer = stepReportSynchronizer;
    this.buffer = buffer;
  }

  public int getNumber() {
    return number;
  }

  public boolean isFree() {
    return isFree;
  }

  public void requestProcessing(Queue<Request> requests, int packageNumber) {
    synchronized (stepReportSynchronizer) {
      try {
        stepReportSynchronizer.wait();
      } catch (InterruptedException ignored) {
      }
    }
    this.requestsPackage = requests;
    this.packageNumber = packageNumber;
    synchronized (newPackageNotifier) {
      System.out.println("Device " + number + ": start process package " + packageNumber);
      isFree = false;
      newPackageNotifier.notify();
    }
  }

  @Override
  public void run() {
    long startBusyTime = 0;
    long startDownTime = 0;
    while (!Thread.currentThread().isInterrupted()) {
      synchronized (newPackageNotifier) {
        try {
          startDownTime = System.currentTimeMillis();
          newPackageNotifier.wait();
          startBusyTime = System.currentTimeMillis();
          report.addDeviceDownTime(number, startBusyTime - startDownTime);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          report.addDeviceDownTime(number, System.currentTimeMillis() - startDownTime);
          break;
        }
      }

      Request request = null;
      for (int i = 0; i < requestsPackage.size() && !Thread.currentThread().isInterrupted(); i++) {
        request = requestsPackage.poll();

        try {
          Thread.sleep((long) (Math.random() * (BETA - ALPHA + 1)) + ALPHA);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }

        report.incrementProcessedRequestCount(request.getSourceNumber());
        System.out.println("    Request " + request.getNumber() + ", package " + packageNumber + ", done");
      }

      report.addRequestServiceTime(request.getSourceNumber(), System.currentTimeMillis() - request.getArrivalTime());

      report.addDeviceBusyTime(number, System.currentTimeMillis() - startBusyTime);
      System.out.println("Device " + number + ": finish process package " + packageNumber);
      buffer.packageIsAvailable(packageNumber);
      isFree = true;
    }
  }
}
