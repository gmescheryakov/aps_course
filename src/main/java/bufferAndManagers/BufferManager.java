package bufferAndManagers;

import source.Request;
import tools.Report;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Queue;

import static tools.ConstantsAndParameters.PACKAGE_SIZE;

public class BufferManager {
    private final Buffer buffer;
    private final Report report;
    private DefaultTableModel modelStepBuf1;

    public BufferManager(Buffer buffer, Report report, DefaultTableModel modelStepBuf1) {
        this.buffer = buffer;
        this.report = report;
        this.modelStepBuf1 = modelStepBuf1;
    }

    public void emplace(Request request) {
        synchronized (buffer) {
            List<Queue<Request>> requestsList = buffer.getRequestsList();
            for (int i = 0; i < requestsList.size(); i++) {
                if (requestsList.get(i) != null && requestsList.get(i).size() != PACKAGE_SIZE) {
                    placementWithRefuse(request, i);
                    return;
                }
            }
            int minNub = 0;
            int index = 0;
            int ii = 0;
            for (Queue<Request> requests : buffer.getRequestsList()) {
                if (requests != null) {
                    for (Request request1 : requests) {
                        if (request1.getNumber() < minNub || minNub == 0){
                            minNub = request1.getNumber();
                            index = ii;
                        }
                    }
                }
                ++ii;
            }

            for (int i = 0; i < requestsList.size(); i++) {
                if (requestsList.get(i) != null && i == index) {
                    placementWithRefuse(request, i);
                    return;
                }
            }
            report.incrementRejectedRequestCount(request.getSourceNumber());
            Object[] object = new Object[1];
            object[0] = "Request " + request.getNumber() + " refused";
            modelStepBuf1.addRow(object);
//      System.out.println("Request " + request.getNumber() + " refused");
        }
    }

    private void placementWithRefuse(Request request, int i) {
        Request oldRequest = buffer.put(i, request);
        if (oldRequest != null) {
            report.incrementRejectedRequestCount(oldRequest.getSourceNumber());
            report.addRequestTimeInBuffer(oldRequest.getSourceNumber(), System.currentTimeMillis() - oldRequest.getArrivalTime());
            Object[] object = new Object[1];
            object[0] = "Request " + oldRequest.getNumber() + " refused";
            modelStepBuf1.addRow(object);
//      System.out.println("Request " + oldRequest.getNumber() + " refused");
        }
    }
}
