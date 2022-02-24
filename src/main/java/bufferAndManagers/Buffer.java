package bufferAndManagers;

import source.Request;

import javax.swing.table.DefaultTableModel;
import java.util.*;

import static tools.ConstantsAndParameters.PACKAGE_SIZE;

public class Buffer {
    private DefaultTableModel modelStepBuf1;
    private DefaultTableModel modelStepBuf2;
    public static boolean isTrue1 = true;
    public static boolean isTrue2 = true;

    private final List<Queue<Request>> buffer;


    private final Object bufferNotEmptyNotifier;

    public Buffer(int size, Object bufferNotEmptyNotifier, DefaultTableModel modelStepBuf1, DefaultTableModel modelStepBuf2) {
        this.buffer = new Vector<>(size);
//    modelStepBuf = new DefaultTableModel();
//    stepmodeTable.setModel(modelStepBuf);
        this.modelStepBuf1 = modelStepBuf1;
        this.modelStepBuf2 = modelStepBuf2;

        for (int i = 0; i < size; i++) {
            buffer.add(i, new ArrayDeque<>());
        }

        this.bufferNotEmptyNotifier = bufferNotEmptyNotifier;
    }

    public Queue<Request> getPackage(int index) {
        Queue<Request> requests = buffer.get(index);
        buffer.set(index, null);
        if (isTrue2) {
            modelStepBuf2.addColumn("request package taken for processing");
            isTrue2 = false;
        }
//    modelStepBuf.addRow(index);
        Object[] obj = new Object[1];
        obj[0] = index;
        modelStepBuf2.addRow(obj);
//        System.out.println("Buffer  : request package " + index + " taken for processing");
        return requests;
    }

    public Request put(int packageNumber, Request request) {
        Queue<Request> requestsPackage = buffer.get(packageNumber);
        Request oldRequest = null;

        if (requestsPackage.size() == PACKAGE_SIZE) {
            oldRequest = requestsPackage.poll();
        }
        requestsPackage.add(request);

        synchronized (bufferNotEmptyNotifier) {
            bufferNotEmptyNotifier.notify();
        }

        if (isTrue1) {
            modelStepBuf1.addColumn("Buffer");
            isTrue1 = false;
        }
        Object[] obj = new Object[1];
        obj[0] = "request " + request.getNumber() + " placed";
        modelStepBuf1.addRow(obj);
//        System.out.println("Buffer  : request " + request.getNumber() + " placed");
        return oldRequest;
    }

    public void packageIsAvailable(int index) {
        buffer.set(index, new ArrayDeque<>());
    }

    public synchronized boolean isEmpty() {
        for (Queue<Request> requests : buffer) {
            if (requests != null && !requests.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    List<Queue<Request>> getRequestsList() {
        return buffer;
    }

    public int getRequestFromBuffer(int index){
        for (Request requests: buffer.get(index)){
            return requests.getSourceNumber();
        }
        return index;
    }
}
