package tools;

import bufferAndManagers.DeviceManager;
import gui.Mainform;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import gui.Mainform.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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
    public static boolean isTrue = true;

    private DefaultTableModel model;
    private DefaultTableModel model2;
    private DefaultTableModel modelSource;
    private DefaultTableModel modelDevice;
    private DefaultTableModel modelBuffer;
    private DefaultTableModel modelStepmode;

    public Report(int sourceCount, int deviceCount, int bufferSize, String reportFileName, JTable autoTable,
                  JTable autoTable2, DefaultTableModel modelSource, DefaultTableModel modelDevice, DefaultTableModel modelBuffer, DefaultTableModel modelStepmode) {

        model = new DefaultTableModel();
        model2 = new DefaultTableModel();


        autoTable.setModel(model);
        autoTable2.setModel(model2);

        this.sourceReports = new Vector<>(sourceCount);
        this.deviceReports = new Vector<>(deviceCount);
        this.bufferSize = bufferSize;
        this.modelSource = modelSource;
        this.modelDevice = modelDevice;
        this.modelBuffer = modelBuffer;
        this.modelStepmode = modelStepmode;

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

        Object[] sourceCells = new Object[sourceReports.size()];
        Object[] deviceCells = new Object[deviceReports.size()];
        Object[] bufferCells = new Object[bufferSize];
        if (isTrue) {
            for (int i = 0; i < sourceReports.size(); i++) {
                modelSource.addColumn("Source " + i);
            }

            for (int i = 0; i < deviceReports.size(); i++) {
                modelDevice.addColumn("Device " + i);
            }

            for (int i = 0; i < bufferSize; i++) {
                modelBuffer.addColumn("Buffer " + i);
            }
            isTrue = false;
        }


        for (SourceReport sourceReport : sourceReports) {
            sourceCells[sourceReport.getNumber()] = sourceReport.getGeneratedRequestCount();
        }
        modelSource.addRow(sourceCells);

        StringBuilder stringBuilder = new StringBuilder();

//        stringBuilder.append(String.format("%10s", "Source"))
//                .append(String.format(" | %24s", "Generated request count"))
//                .append(String.format(" | %24s", "Rejected request count"))
//                .append(String.format(" %2s", "|\n"))
//                .append("------------------------------------------------------------------\n");
//
//        for (SourceReport sourceReport : sourceReports) {
//
//            stringBuilder.append(String.format("%10s", "Source " + sourceReport.getNumber()))
//                    .append(String.format(" | %24s", sourceReport.getGeneratedRequestCount()))
//                    .append(String.format(" | %24s", sourceReport.getRejectedRequestCount()))
//                    .append(String.format(" %2s", "|\n"));
//        }
//        stringBuilder.append("------------------------------------------------------------------\n");

        if (deviceManager != null){
            int i = 0;
            for (Boolean status : deviceManager.getDeviceStatuses()) {

                if (status){
                    deviceCells[i] = "Free";
                }
                else{
                    deviceCells[i] = "Busy";
                }
                if (deviceManager.getDevicePointer() == i) {
                    deviceCells[i] += "*";
                }
                i++;
            }

        }
        modelDevice.addRow(deviceCells);

        if (deviceManager != null) {
//            stringBuilder.append(String.format("\n%10s", "Device"))
//                    .append(String.format(" | %7s", "Status"))
//                    .append(String.format(" %2s", "|\n"))
//                    .append("----------------------\n");
//            ;
//            int i = 0;
//            for (Boolean status : deviceManager.getDeviceStatuses()) {
//                stringBuilder
//                        .append(String.format("%10s", (deviceManager.getDevicePointer() == i ? "*" : "") + "Device " + i++))
//                        .append(String.format(" | %7s", status ? "Free" : "Busy"))
//                        .append(String.format(" %2s", "|\n"));
//            }
//            stringBuilder.append("----------------------\n");
//

            deviceManager.bufferOutput(modelBuffer);
        }

        return stringBuilder.toString();
    }

    public synchronized void writeFileStepReport() throws IOException {
        String[] cells = new String[10];

        cells[0] = "Source number";
        cells[1] = "Generated request count";
        cells[2] = "Processed request count";
        cells[3] = "Rejected request count";
        cells[4] = "Failure probability";
        cells[5] = "Requests time in buffer";
        cells[6] = "Requests service time";
        cells[7] = "Total time in system";
        cells[8] = "Variance of time in buffer";
        cells[9] = "Variance of service time";

        for (String column : cells) {
            model2.addColumn(column);
        }
        Object[] dataT1 = new Object[cells.length];

        for (SourceReport report : sourceReports) {
            dataT1[0] = "Source " + report.getNumber();
            dataT1[1] = report.getGeneratedRequestCount();
            dataT1[2] = report.getProcessedRequestCount();
            dataT1[3] = report.getRejectedRequestCount();
            dataT1[4] = report.getProcessedRequestCount() + report.getRejectedRequestCount() == 0
                    ? 0
                    : (double) report.getRejectedRequestCount() / (report.getProcessedRequestCount() + report.getRejectedRequestCount());
            dataT1[5] = report.getRequestTimeInBuffer().doubleValue();
            dataT1[6] = report.getRequestServiceTime().doubleValue();
            dataT1[7] = report.getRequestServiceTime().add(report.getRequestTimeInBuffer()).doubleValue();
            model2.addRow(dataT1);

        }

        //Variance

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


        dataT1[0] = "Total";
        dataT1[8] = sourceReports.size() <= 1
                ? 0
                : sumBf.divide(BigDecimal.valueOf(sourceReports.size() - 1), 5, RoundingMode.HALF_EVEN).doubleValue();
        dataT1[9] = sourceReports.size() <= 1
                ? 0
                : sumServ.divide(BigDecimal.valueOf(sourceReports.size() - 1), 5, RoundingMode.HALF_EVEN).doubleValue();

        model2.addRow(dataT1);

        model2.addRow(new Object[10]);

        dataT1 = new Object[cells.length];

        dataT1[0] = "Device number";
        dataT1[1] = "Use factor";

        model2.addRow(dataT1);

        dataT1 = new Object[10];
        for (DeviceReport report : deviceReports) {
            dataT1[0] = "Device " + report.getNumber();
            dataT1[1] = report.getUseFactor();
            model2.addRow(dataT1);
        }
    }

    public synchronized void writeTotalReport() throws IOException {
        calculate();

        String[] cells = new String[9];
        cells[0] = "Source count";
        cells[1] = "Device count";
        cells[2] = "Packages count";
        cells[3] = "Package size";
        cells[4] = "Total request count";
        cells[5] = "Failure probability";
        cells[6] = "Average request time in system";
        cells[7] = "System work time";
        cells[8] = "System workload";

        for (String column : cells) {
            model.addColumn(column);
        }

        Object[] dataT1 = new Object[cells.length];

        dataT1[0] = sourceReports.size();
        dataT1[1] = deviceReports.size();
        dataT1[2] = bufferSize;
        dataT1[3] = PACKAGE_SIZE;
        dataT1[4] = totalRequestCount;
        dataT1[5] = failureProbability;
        dataT1[6] = averageRequestTimeInSystem;
        dataT1[7] = totalDeviceBusyTime.add(totalDeviceDownTime).longValue();
        dataT1[8] = systemWorkload;
        model.addRow(dataT1);

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
