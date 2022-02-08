package source;

public class Request {
  private static int counter;

  private final int number;
  private final int sourceNumber;
  private final long arrivalTime;

  public Request(int sourceNumber) {
    this.number = counter++;
    this.sourceNumber = sourceNumber;
    this.arrivalTime = System.currentTimeMillis();
  }

  public int getNumber() {
    return number;
  }

  public int getSourceNumber() {
    return sourceNumber;
  }

  public long getArrivalTime() {
    return arrivalTime;
  }
}
