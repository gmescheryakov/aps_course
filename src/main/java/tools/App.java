package tools;

import bufferAndManagers.Buffer;
import bufferAndManagers.BufferManager;
import bufferAndManagers.DeviceManager;
import device.Device;
import gui.Mainform;
import source.Source;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import static tools.ConstantsAndParameters.*;

public class App {
    public static void goWork(boolean stepMode, int BUFFER_SIZE, int SOURCES_COUNT, int DEVICES_COUNT, double lambda,
                            JTable autoTable, JTable autoTable2, DefaultTableModel modelSource, DefaultTableModel modelDevice,
                              DefaultTableModel modelBuffer, DefaultTableModel modelStepmode, Object stepReportSynchronizer,
                              DefaultTableModel modelStepBuf1, DefaultTableModel modelStepBuf2, DefaultTableModel modelStepSource, DefaultTableModel modelStepDevice)
    {
        Thread thread = new Thread(()->{mainMethod(stepMode, BUFFER_SIZE, SOURCES_COUNT, DEVICES_COUNT, lambda, autoTable, autoTable2, modelSource, modelDevice, modelBuffer, modelStepmode, stepReportSynchronizer, modelStepBuf1, modelStepBuf2, modelStepSource, modelStepDevice);});
        thread.start();
    }
    public static void mainMethod(boolean stepMode, int BUFFER_SIZE, int SOURCES_COUNT, int DEVICES_COUNT, double lambda,
                                  JTable autoTable, JTable autoTable2, DefaultTableModel modelSource,
                                  DefaultTableModel modelDevice, DefaultTableModel modelBuffer, DefaultTableModel modelStepmode,
                                  Object next, DefaultTableModel modelStepBuf1, DefaultTableModel modelStepBuf2, DefaultTableModel modelStepSource, DefaultTableModel modelStepDevice) {
        Object stepReportSynchronizer = new Object();
        LAMBDA = lambda;
        final int SIMULATION_TIME = 20 * MILLISECONDS_PER_SECOND;

        Scanner in = new Scanner(System.in);

        Report report = new Report(SOURCES_COUNT, DEVICES_COUNT, BUFFER_SIZE, "Report.xls", autoTable, autoTable2,
                 modelSource, modelDevice, modelBuffer, modelStepmode);

        //Buffer
        Object bufferNotEmptyNotifier = new Object();
        Buffer buffer = new Buffer(BUFFER_SIZE, bufferNotEmptyNotifier, modelStepBuf1, modelStepBuf2);

        //Buffer Manager
        BufferManager bufferManager = new BufferManager(buffer, report, modelStepBuf1);

        //Sources
        List<Thread> sourcesThreads = new ArrayList<>(SOURCES_COUNT);
        for (int i = 0; i < SOURCES_COUNT; i++) {
            sourcesThreads.add(new Thread(new Source(bufferManager, report, stepReportSynchronizer, modelStepSource)));
        }

        //Devices
        Vector<Device> devices = new Vector<>(DEVICES_COUNT);
        List<Thread> devicesThreads = new ArrayList<>(DEVICES_COUNT);
        for (int i = 0; i < DEVICES_COUNT; i++) {
            Device device = new Device(report, stepReportSynchronizer, buffer, modelStepDevice);
            devices.add(device);
            devicesThreads.add(new Thread(device));
        }

        //Device Manager
        DeviceManager deviceManager = new DeviceManager(buffer, devices, bufferNotEmptyNotifier, stepReportSynchronizer);
        Thread deviceManagerThread = new Thread(deviceManager);

        report.setDeviceManager(deviceManager);

        //Start threads
        try {
            devicesThreads.forEach(Thread::start);
            Thread.sleep(1);
            sourcesThreads.forEach(Thread::start);
            Thread.sleep(1);
            deviceManagerThread.start();

            if (stepMode) {
                long userInputWaitTime = 0;
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < SIMULATION_TIME + userInputWaitTime) {
                    synchronized (stepReportSynchronizer) {
                        stepReportSynchronizer.notify();
                    }
                    long startUserInputTime = System.currentTimeMillis();
                    Thread.sleep(10);
//                    System.out.println(report.writeConsoleStepReport());
//                    System.out.print("Simulation stopped, wait input: \n");
//                    in.nextLine();
                    synchronized (next){
                        report.writeConsoleStepReport();
                        next.wait();
                    }
                    userInputWaitTime += System.currentTimeMillis() - startUserInputTime;
                }
            } else {
                Thread thread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        synchronized (stepReportSynchronizer) {
                            stepReportSynchronizer.notify();
                        }
                    }
                });
                thread.start();
                Thread.sleep(SIMULATION_TIME);
                thread.interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sourcesThreads.forEach(Thread::interrupt);
        devicesThreads.forEach(Thread::interrupt);
        deviceManagerThread.interrupt();

        try {
            for (Thread sourceThread : sourcesThreads) {
                sourceThread.join();
            }
            for (Thread deviceThread : devicesThreads) {
                deviceThread.join();
            }
            deviceManagerThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupt");
        }

        try {
            report.writeFileStepReport();
            report.writeTotalReport();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Device.resetCounter();
        Source.resetCounter();
        Report.isTrue = false;
        Source.isTrue = false;
//        Buffer.isTrue1 = false;
//        Buffer.isTrue2 = false;
        Device.isTrue = false;
    }
}
