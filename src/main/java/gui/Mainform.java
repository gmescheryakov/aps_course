package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static tools.App.*;

public class Mainform extends JFrame {
    private JPanel mainPanel;
    private JTextField sourceCount;
    private JTextField bufferCount;
    private JTextField deviceCount;
    private JTextField lambda;
    private JButton startButton;
    private JButton nextButton;
//    private JButton resetButton;
    private JCheckBox stepmodeCheckBox;
    private JPanel autoModePanel;
    private JTable autoTable;
    private JTable autoTable2;
    private JTable stepmodeSourceTable;
    private JTable stepmodeBufferTable;
    private JTable stepmodeDeviceTable;
    private JTable stepmodeTable;
    private JPanel stepmodePanel;
    private JTable stepmodeBuffer1;
    private JTable stepmodeBuffer2;
    private JTable stepmodeDeviceInfo;
    private DefaultTableModel modelSource;
    private DefaultTableModel modelDevice;
    private DefaultTableModel modelBuffer;
    private DefaultTableModel modelStepmode;
    private DefaultTableModel modelStepBuf1;
    private DefaultTableModel modelStepBuf2;
    private DefaultTableModel modelStepSource;
    private DefaultTableModel modelStepDevice;

    private int source;
    private int buffer;
    private int device;
    private double lamb;
    private boolean stepmodeBuffer11;
    private Object next;
//    private Boolean isReset = false;


    public Mainform() {
        setContentPane(mainPanel);
        setTitle("Welcome");
        setSize(1000, 800);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        sourceCount.setText("5");
        bufferCount.setText("23");
        deviceCount.setText("4");
        lambda.setText("0.26");
        next = new Object();

        modelSource = new DefaultTableModel();
        modelDevice = new DefaultTableModel();
        modelBuffer = new DefaultTableModel();
        modelStepmode = new DefaultTableModel();
        modelStepBuf1 = new DefaultTableModel();
        modelStepBuf2 = new DefaultTableModel();
        modelStepSource = new DefaultTableModel();
        modelStepDevice = new DefaultTableModel();

        stepmodeDeviceTable.setModel(modelDevice);
        stepmodeBufferTable.setModel(modelBuffer);
        stepmodeSourceTable.setModel(modelSource);
        stepmodeTable.setModel(modelStepSource);
        stepmodeBuffer1.setModel(modelStepBuf1);
        stepmodeBuffer2.setModel(modelStepBuf2);
        stepmodeDeviceInfo.setModel(modelStepDevice);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        nextButton.setVisible(false);
//        resetButton.setVisible(false);
        stepmodeCheckBox.setSelected(false);
        autoModePanel.setVisible(false);
        stepmodePanel.setVisible(false);


        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!stepmodeCheckBox.isSelected()) {
                    autoModePanel.setVisible(true);
                }
                else {
                    nextButton.setVisible(true);
                    stepmodePanel.setVisible(true);
                    for (int i = 0; i < source; i++){
                        modelSource.addColumn("Source " + i);
                    }
                    modelSource.addRow(new Object[source]);

                    for (int i = 0; i < device; i++){
                        modelDevice.addColumn("Device " + i);
                    }

                    for (int i = 0; i < buffer; i++){
                        modelBuffer.addColumn("Buffer " + i);
                    }

                }
                startButton.setVisible(false);
                stepmodeCheckBox.setVisible(false);
//                resetButton.setVisible(true);

                stepmodeBuffer11 = stepmodeCheckBox.isSelected();
                source = Integer.parseInt(sourceCount.getText());
                buffer = Integer.parseInt(bufferCount.getText());
                device = Integer.parseInt(deviceCount.getText());
                lamb =Double.parseDouble(lambda.getText());
                goWork(stepmodeBuffer11, buffer, source, device, lamb, autoTable, autoTable2, modelSource, modelDevice, modelBuffer, modelStepmode, next, modelStepBuf1, modelStepBuf2, modelStepSource, modelStepDevice);
            }
        });
//        resetButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                stepmodePanel.setVisible(false);
//                autoModePanel.setVisible(false);
//                nextButton.setVisible(false);
//                resetButton.setVisible(false);
//                startButton.setVisible(true);
//                stepmodeCheckBox.setVisible(true);
//                isReset = true;
//
//            }
//        });


        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (next) {
                    next.notify();
                }
            }
        });
    }
}
