package bufferAndManagers;

import source.Request;
import tools.Report;

import java.util.List;
import java.util.Queue;

import static tools.ConstantsAndParameters.PACKAGE_SIZE;

public class BufferManager {
  private final Buffer buffer;
  private final Report report;

  public BufferManager(Buffer buffer, Report report) {
    this.buffer = buffer;
    this.report = report;
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
      for (int i = 0; i < requestsList.size(); i++) {
        if (requestsList.get(i) != null) {
          placementWithRefuse(request, i);
          return;
        }
      }
      report.incrementRejectedRequestCount(request.getSourceNumber());
      System.out.println("Request " + request.getNumber() + " refused");
    }
  }

  private void placementWithRefuse(Request request, int i) {
    Request oldRequest = buffer.put(i, request);
    if (oldRequest != null) {
      report.incrementRejectedRequestCount(oldRequest.getSourceNumber());
      report.addRequestTimeInBuffer(oldRequest.getSourceNumber(),System.currentTimeMillis() - oldRequest.getArrivalTime());
      System.out.println("Request " + oldRequest.getNumber() + " refused");
    }
  }
}
