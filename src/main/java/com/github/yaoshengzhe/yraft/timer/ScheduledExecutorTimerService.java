package com.github.yaoshengzhe.yraft.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorTimerService implements TimerService {

  private Runnable timeoutTask = null;
  private ScheduledExecutorService service;
  private ScheduledFuture currentTask;
  private final long delayInMilli;
  private boolean stopped = false;

  public ScheduledExecutorTimerService(long delay, TimeUnit unit) {
    this.delayInMilli = unit.toMillis(delay);
  }

  @Override
  public void init(final Runnable runnable) {
    if (timeoutTask != null) {
      throw new IllegalStateException("Cannot call TimerService.init more than once.");
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
    }

    if (!stopped) {
      this.currentTask = this.service.schedule(this.timeoutTask, delayInMilli, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public void stop() {
    stopped = true;
  }
}
