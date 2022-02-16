package device;

import bufferAndManagers.Buffer;
import source.Request;
import tools.Report;

import javax.swing.table.DefaultTableModel;
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
  private DefaultTableModel modelStepDevice;
  public static boolean isTrue = true;

  private final Buffer buffer;

  public Device(Report report, Object stepReportSynchronizer, Buffer buffer, DefaultTableModel modelStepDevice) {
    this.number = count++;
    this.report = report;
    this.isFree = true;
    this.newPackageNotifier = new Object();
    this.stepReportSynchronizer = stepReportSynchronizer;
    this.buffer = buffer;
    this.modelStepDevice = modelStepDevice;
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
      if (isTrue) {
        modelStepDevice.addColumn("Device info");
        isTrue = false;
      }
      Object[] objects = new Object[1];
      objects[0] = "Device " + number + ": start process package " + packageNumber;
      modelStepDevice.addRow(objects);
//      System.out.println("Device " + number + ": start process package " + packageNumber);
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
        Object[] objects = new Object[1];
        objects[0] = "Request " + request.getNumber() + ", package " + packageNumber + ", done";
        modelStepDevice.addRow(objects);
//        System.out.println("    Request " + request.getNumber() + ", package " + packageNumber + ", done");
      }

      report.addRequestServiceTime(request.getSourceNumber(), System.currentTimeMillis() - request.getArrivalTime());

      report.addDeviceBusyTime(number, System.currentTimeMillis() - startBusyTime);
      Object[] objects1 = new Object[1];
      objects1[0] = "Device " + number + ": finish process package " + packageNumber;
      modelStepDevice.addRow(objects1);
//      System.out.println("Device " + number + ": finish process package " + packageNumber);
      buffer.packageIsAvailable(packageNumber);
      isFree = true;
    }
  }

  public static void resetCounter(){
    count = 0;
  }
}
