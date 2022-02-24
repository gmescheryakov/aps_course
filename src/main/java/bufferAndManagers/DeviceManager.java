package bufferAndManagers;

import com.microsoft.schemas.compatibility.AlternateContentDocument;
import device.Device;
import org.apache.commons.math3.util.Pair;
import source.Request;
import tools.Report;
import bufferAndManagers.Buffer;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import static tools.ConstantsAndParameters.PACKAGE_SIZE;

public class DeviceManager implements Runnable {
  private final Vector<Device> devices;
  private final Buffer buffer;
  private volatile int devicePointer;
  private static int sourcePointer = 0;

  private final Object bufferNotEmptyNotifier;
  private final Object stepReportSynchronizer;

  public DeviceManager(Buffer buffer, Vector<Device> devices, Object bufferNotEmptyNotifier, Object stepReportSynchronizer) {
    this.devices = devices;
    this.buffer = buffer;
    this.bufferNotEmptyNotifier = bufferNotEmptyNotifier;
    this.stepReportSynchronizer = stepReportSynchronizer;
  }

  public List<Boolean> getDeviceStatuses() {
    List<Boolean> deviceStatuses = new ArrayList<>(devices.size());
    synchronized (devices) {
      for (Device device : devices) {
        deviceStatuses.add(device.isFree());
      }
    }
    return deviceStatuses;
  }

  public int getDevicePointer() {
    return devicePointer;
  }

  public void bufferOutput(DefaultTableModel modelBuffer) {
    synchronized (buffer.getRequestsList()) {
      Object[] bufferCells = new Object[buffer.getRequestsList().size()];
      int i = 0;
      for (int j = 0; j < buffer.getRequestsList().size(); j++){
        bufferCells[j] = "";
      }
      for (Queue<Request> requests : buffer.getRequestsList()) {
        if (requests != null) {
          for (Request request : requests) {
            bufferCells[i] += (request == null ? "" : (request.getNumber()) + " ");
          }
        }
        ++i;
      }
      modelBuffer.addRow(bufferCells);

    }
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      if (buffer.isEmpty()) {
        System.out.println("Buffer empty");
        try {
          synchronized (bufferNotEmptyNotifier) {
            bufferNotEmptyNotifier.wait();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      Device device = null;
      try {
        device = selectDevice();
      } catch (Exception e) {
        continue;
      }

      synchronized (stepReportSynchronizer) {
        try {
          stepReportSynchronizer.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      Pair<Integer, Queue<Request>> requests = null;
      try {
        requests = selectRequestPackage();
      } catch (Exception e) {
        System.out.println(e.getMessage());
        continue;
      }

      device.requestProcessing(requests.getSecond(), requests.getFirst());
    }
  }

  private Device selectDevice() throws Exception {
    Device device = null;
    synchronized (devices) {
      for (int i = devicePointer; i < devices.size(); i++) {
        if ((device = devices.get(i)).isFree()) {
          devicePointer = i;
          return device;
        }
      }
      for (int i = 0; i < devicePointer; i++) {
        if ((device = devices.get(i)).isFree()) {
          devicePointer = i;
          return device;
        }
      }
    }
    throw new Exception("No free devices");
  }

  private Pair<Integer, Queue<Request>> selectRequestPackage() throws Exception {
    Queue<Request> requests = null;
    synchronized (buffer.getRequestsList()) {
      List<Queue<Request>> requestsPackages = buffer.getRequestsList();
      boolean isEnd = false;
      for(int j = 0; j < requestsPackages.size(); j++) {
        for (int i = 0; i < requestsPackages.size(); i++) {
          if (requestsPackages.get(i) != null && !requestsPackages.get(i).isEmpty() && sourcePointer == buffer.getRequestFromBuffer(i)) {
            requests = buffer.getPackage(i);
            return new Pair<>(i, requests);
          }
        }
        sourcePointer++;
        if (isEnd == false){
          isEnd = true;
          sourcePointer = 0;
          j = 0;
        }
      }

    }
    throw new Exception("No requests in buffer");
  }

//  private int getRequestFromBuffer(int index){
//    for (Request requests: buffer.get(index)){
//      return requests.getSourceNumber();
//    }
//    return index;
//  }
}
