package org.yraft.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledDelayTimerService implements TimerService {

  private Runnable timeoutTask = null;
  private ScheduledExecutorService service;
  private ScheduledFuture currentTask;
  private long delayInMilli;
  private boolean stopped = false;

  public ScheduledDelayTimerService(long delay, TimeUnit unit, final Runnable runnable) {
    this.delayInMilli = unit.toMillis(delay);
    if (timeoutTask != null) {
      throw new IllegalStateException("Cannot call TimerService.setRunnable more than once.");
    }
    this.timeoutTask = new Runnable() {
      @Override
      public void run() {
        runnable.run();
      }
    };
    this.service = Executors.newSingleThreadScheduledExecutor();
    this.stopped = false;
  }

  @Override
  public void reset() {
    reschedule();
  }

  private void reschedule() {
    if (this.currentTask != null) {
      this.currentTask.cancel(false);
      this.currentTask = null;
    }

    if (!stopped) {
      this.currentTask = this.service.schedule(this.timeoutTask, delayInMilli, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public void start() {
    if (stopped) {
      stopped = false;
      this.reschedule();
    }
  }
  @Override
  public void stop() {
    if (!stopped) {
      stopped = true;
      this.reschedule();
    }
  }

  public void setDelay(long delay, TimeUnit unit) {
    this.delayInMilli = unit.toMillis(delay);
  }
}
