package tools;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeviceReport {
  private final int number;
  private BigDecimal downTime;
  private BigDecimal busyTime;

  public DeviceReport(int number) {
    this.number = number;
    this.downTime = new BigDecimal(0);
    this.busyTime = new BigDecimal(0);
  }

  public synchronized int getNumber() {
    return number;
  }

  public synchronized BigDecimal getDownTime() {
    return downTime;
  }

  public synchronized BigDecimal getBusyTime() {
    return busyTime;
  }

  public synchronized double getUseFactor() {
    return busyTime.longValue() == 0 && downTime.longValue() == 0
        ? 0
        : busyTime.divide(busyTime.add(downTime), 5, RoundingMode.HALF_EVEN).doubleValue();
  }

  public synchronized void addDownTime(long downTime) {
    this.downTime = this.downTime.add(BigDecimal.valueOf(downTime));
  }

  public synchronized void addBusyTime(long busyTime) {
    this.busyTime = this.busyTime.add(BigDecimal.valueOf(busyTime));
  }
}
