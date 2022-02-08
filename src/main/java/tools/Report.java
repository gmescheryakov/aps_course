package tools;

import bufferAndManagers.DeviceManager;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import static tools.ConstantsAndParameters.PACKAGE_SIZE;

public class Report {
  private DeviceManager deviceManager;

  private final List<DeviceReport> deviceReports;
  private final List<SourceReport> sourceReports;
  private final int bufferSize;

  private int totalRequestCount;
  private double failureProbability;
  private BigDecimal totalRequestTimeInSystem;
  private BigDecimal totalDeviceDownTime;
  private BigDecimal totalDeviceBusyTime;

  private double systemWorkload;
  private double averageRequestTimeInSystem;

  private final Workbook workbook;
  private int stepCount;
  private final String reportFileName;

  public Report(int sourceCount, int deviceCount, int bufferSize, String reportFileName) {
    this.sourceReports = new Vector<>(sourceCount);
    this.deviceReports = new Vector<>(deviceCount);
    this.bufferSize = bufferSize;

    for (int i = 0; i < sourceCount; i++) {
      sourceReports.add(new SourceReport(i));
    }
    for (int i = 0; i < deviceCount; i++) {
      deviceReports.add(new DeviceReport(i));
    }

    this.totalRequestTimeInSystem = new BigDecimal(0);
    this.totalDeviceDownTime = new BigDecimal(0);
    this.totalDeviceBusyTime = new BigDecimal(0);

    //Create table
    this.workbook = new HSSFWorkbook();

    this.reportFileName = reportFileName;
  }

  public void setDeviceManager(DeviceManager deviceManager) {
    this.deviceManager = deviceManager;
  }

  //Source reports
  public void incrementGeneratedRequestCount(int number) {
    sourceReports.get(number).incrementGeneratedRequestCount();
  }

  public void incrementProcessedRequestCount(int number) {
    sourceReports.get(number).incrementProcessedRequestCount();
  }

  public void incrementRejectedRequestCount(int number) {
    sourceReports.get(number).incrementRejectedRequestCount();
  }

  public void addRequestServiceTime(int number, long requestServiceTime) {
    sourceReports.get(number).addRequestServiceTime(requestServiceTime);
  }

  public void addRequestTimeInBuffer(int number, long requestTimeInBuffer) {
    sourceReports.get(number).addRequestTimeInBuffer(requestTimeInBuffer);
  }

  //Device reports
  public void addDeviceDownTime(int number, long downTime) {
    deviceReports.get(number).addDownTime(downTime);
  }

  public void addDeviceBusyTime(int number, long busyTime) {
    deviceReports.get(number).addBusyTime(busyTime);
  }

  public synchronized String writeConsoleStepReport() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(String.format("%10s", "Source"))
            .append(String.format(" | %24s", "Generated request count"))
            .append(String.format(" | %24s", "Rejected request count"))
            .append(String.format(" %2s", "|\n"))
            .append("------------------------------------------------------------------\n");

    for (SourceReport sourceReport : sourceReports) {

      stringBuilder.append(String.format("%10s", "Source " + sourceReport.getNumber()))
              .append(String.format(" | %24s", sourceReport.getGeneratedRequestCount()))
              .append(String.format(" | %24s", sourceReport.getRejectedRequestCount()))
              .append(String.format(" %2s", "|\n"));
    }
    stringBuilder.append("------------------------------------------------------------------\n");


    if (deviceManager != null) {
      stringBuilder.append(String.format("\n%10s", "Device"))
              .append(String.format(" | %7s", "Status"))
              .append(String.format(" %2s", "|\n"))
              .append("----------------------\n");;
      int i = 0;
      for (Boolean status : deviceManager.getDeviceStatuses()) {
        stringBuilder
                .append(String.format("%10s", (deviceManager.getDevicePointer() == i ? "*" : "") + "Device " + i++))
                .append(String.format(" | %7s", status ? "Free" : "Busy"))
                .append(String.format(" %2s", "|\n"));
      }
      stringBuilder.append("----------------------\n");

      stringBuilder.append("\nBuffer\n")
              .append(deviceManager.bufferOutput());
    }

    return stringBuilder.toString();
  }

  public synchronized void writeFileStepReport() throws IOException {
    Sheet sheet = workbook.createSheet("Step report " + (++stepCount));
    int rowCounter = 0;
    Row row = sheet.createRow(rowCounter++);
    List<Cell> cells = new ArrayList<>(6);
    for (int i = 0; i < 10; i++) {
      cells.add(row.createCell(i));
    }
    cells.get(0).setCellValue("Source number");
    cells.get(1).setCellValue("Generated request count");
    cells.get(2).setCellValue("Processed request count");
    cells.get(3).setCellValue("Rejected request count");
    cells.get(4).setCellValue("Failure probability");
    cells.get(5).setCellValue("Requests time in buffer");
    cells.get(6).setCellValue("Requests service time");
    cells.get(7).setCellValue("Total time in system");
    cells.get(8).setCellValue("Variance of time in buffer");
    cells.get(9).setCellValue("Variance of service time");

    for (SourceReport report : sourceReports) {
      row = sheet.createRow(rowCounter++);
      cells.clear();
      for (int j = 0; j < 8; j++) {
        cells.add(row.createCell(j));
      }
      cells.get(0).setCellValue("Source " + report.getNumber());
      cells.get(1).setCellValue(report.getGeneratedRequestCount());
      cells.get(2).setCellValue(report.getProcessedRequestCount());
      cells.get(3).setCellValue(report.getRejectedRequestCount());
      cells.get(4).setCellValue(report.getProcessedRequestCount() + report.getRejectedRequestCount() == 0
              ? 0
              : (double) report.getRejectedRequestCount() / (report.getProcessedRequestCount() + report.getRejectedRequestCount()));
      cells.get(5).setCellValue(report.getRequestTimeInBuffer().doubleValue());
      cells.get(6).setCellValue(report.getRequestServiceTime().doubleValue());
      cells.get(7).setCellValue(report.getRequestServiceTime().add(report.getRequestTimeInBuffer()).doubleValue());
    }

    //Variance
    row = sheet.createRow(rowCounter++);
    cells.clear();
    for (int j = 0; j < 10; j++) {
      cells.add(row.createCell(j));
    }

    BigDecimal avgBf = new BigDecimal(0);
    BigDecimal avgServ = new BigDecimal(0);

    for (SourceReport sourceReport : sourceReports) {
      avgBf = avgBf.add(sourceReport.getRequestTimeInBuffer());
      avgServ = avgServ.add(sourceReport.getRequestServiceTime());
    }
    avgBf = avgBf.divide(BigDecimal.valueOf(sourceReports.size()), 5, RoundingMode.HALF_EVEN);
    avgServ = avgServ.divide(BigDecimal.valueOf(sourceReports.size()), 5, RoundingMode.HALF_EVEN);

    BigDecimal sumBf = new BigDecimal(0);
    BigDecimal sumServ = new BigDecimal(0);
    for (SourceReport sourceReport : sourceReports) {
      sumBf = sumBf.add(sourceReport.getRequestTimeInBuffer().subtract(avgBf).pow(2));
      sumServ = sumServ.add(sourceReport.getRequestServiceTime().subtract(avgServ).pow(2));
    }

    cells.get(0).setCellValue("Total");
    cells.get(8).setCellValue(sourceReports.size() <= 1
            ? 0
            : sumBf.divide(BigDecimal.valueOf(sourceReports.size() - 1), 5, RoundingMode.HALF_EVEN).doubleValue());
    cells.get(9).setCellValue(sourceReports.size() <= 1
            ? 0
            : sumServ.divide(BigDecimal.valueOf(sourceReports.size() - 1), 5, RoundingMode.HALF_EVEN).doubleValue());
    sheet.createRow(rowCounter++);

    row = sheet.createRow(rowCounter++);
    cells.clear();
    for (int i = 0; i < 2; i++) {
      cells.add(row.createCell(i));
    }
    cells.get(0).setCellValue("Device number");
    cells.get(1).setCellValue("Use factor");

    for (DeviceReport report : deviceReports) {
      row = sheet.createRow(rowCounter++);
      cells.clear();
      for (int j = 0; j < 2; j++) {
        cells.add(row.createCell(j));
      }
      cells.get(0).setCellValue("Device " + report.getNumber());
      cells.get(1).setCellValue(report.getUseFactor());
    }

    for (int i = 0; i < 10; i++) {
      sheet.autoSizeColumn(i);
    }

    workbook.write(new FileOutputStream(reportFileName));
  }

  public synchronized void writeTotalReport() throws IOException {
    calculate();
    Sheet sheet = workbook.createSheet("Total report");
    int rowCounter = 0;
    Row row = sheet.createRow(rowCounter++);
    List<Cell> cells = new ArrayList<>(8);
    for (int i = 0; i < 9; i++) {
      cells.add(row.createCell(i));
    }
    cells.get(0).setCellValue("Source count");
    cells.get(1).setCellValue("Device count");
    cells.get(2).setCellValue("Packages count");
    cells.get(3).setCellValue("Package size");
    cells.get(4).setCellValue("Total request count");
    cells.get(5).setCellValue("Failure probability");
    cells.get(6).setCellValue("Average request time in system");
    cells.get(7).setCellValue("System work time");
    cells.get(8).setCellValue("System workload");

    row = sheet.createRow(rowCounter++);
    cells.clear();
    for (int i = 0; i < 9; i++) {
      cells.add(row.createCell(i));
      sheet.autoSizeColumn(i);
    }
    cells.get(0).setCellValue(sourceReports.size());
    cells.get(1).setCellValue(deviceReports.size());
    cells.get(2).setCellValue(bufferSize);
    cells.get(3).setCellValue(PACKAGE_SIZE);
    cells.get(4).setCellValue(totalRequestCount);
    cells.get(5).setCellValue(failureProbability);
    cells.get(6).setCellValue(averageRequestTimeInSystem);
    cells.get(7).setCellValue(totalDeviceBusyTime.add(totalDeviceDownTime).longValue());
    cells.get(8).setCellValue(systemWorkload);

    workbook.write(new FileOutputStream(reportFileName));
  }

  //Other
  private synchronized void calculate() {

    AtomicInteger processedRequestCount = new AtomicInteger();
    AtomicInteger rejectedRequestCount = new AtomicInteger();

    sourceReports.forEach(sourceReport -> {
      totalRequestCount += sourceReport.getGeneratedRequestCount();
      processedRequestCount.addAndGet(sourceReport.getProcessedRequestCount());
      rejectedRequestCount.addAndGet(sourceReport.getRejectedRequestCount());
      totalRequestTimeInSystem = totalRequestTimeInSystem.add(sourceReport.getRequestTimeInBuffer().add(sourceReport.getRequestServiceTime()));
    });

    failureProbability = (double) rejectedRequestCount.get() / (rejectedRequestCount.get() + processedRequestCount.get());

    deviceReports.forEach(deviceReport -> {
      totalDeviceBusyTime = totalDeviceBusyTime.add(deviceReport.getBusyTime());
      totalDeviceDownTime = totalDeviceDownTime.add(deviceReport.getDownTime());
    });

    systemWorkload = totalDeviceBusyTime.divide(totalDeviceDownTime.add(totalDeviceBusyTime), 5, RoundingMode.HALF_EVEN).doubleValue();
    averageRequestTimeInSystem = totalRequestTimeInSystem
            .divide(BigDecimal.valueOf(totalRequestCount), 5, RoundingMode.HALF_EVEN).doubleValue();
  }
}
