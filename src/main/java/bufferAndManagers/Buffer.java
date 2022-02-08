package bufferAndManagers;

import source.Request;
import tools.Report;

import java.util.*;

import static tools.ConstantsAndParameters.PACKAGE_SIZE;

public class Buffer {
  private final List<Queue<Request>> buffer;

  private final Object bufferNotEmptyNotifier;

  public Buffer(int size, Object bufferNotEmptyNotifier) {
    this.buffer = new Vector<>(size);

    for (int i = 0; i < size; i++) {
      buffer.add(i, new ArrayDeque<>());
    }

    this.bufferNotEmptyNotifier = bufferNotEmptyNotifier;
  }

  public Queue<Request> getPackage(int index) {
    Queue<Request> requests = buffer.get(index);
    buffer.set(index, null);
    System.out.println("Buffer  : request package " + index + " taken for processing");
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

    System.out.println("Buffer  : request " + request.getNumber() + " placed");
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
}
