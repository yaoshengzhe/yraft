package org.yraft.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HeartbeatTimerService implements TimerService {

    private Runnable timeoutTask = null;
    private ScheduledExecutorService service;
    private ScheduledFuture currentTask;
    private long delayInMilli;

    public HeartbeatTimerService(long delay, TimeUnit unit) {
      this.delayInMilli = unit.toMillis(delay);
      if (timeoutTask != null) {
        throw new IllegalStateException("Cannot call TimerService.setRunnable more than once.");
      }
      this.timeoutTask = new Runnable() {
        @Override
        public void run() {
        }
      };
      this.service = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void reset() {
      throw new UnsupportedOperationException();
    }

    private void reschedule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void start() {
      this.currentTask = this.service.scheduleAtFixedRate(this.timeoutTask, 0, delayInMilli, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
      if (this.currentTask != null) {
        this.currentTask.cancel(false);
        this.currentTask = null;
      }
    }

    @Override
    public long getRecentTimeoutInMills() {
      return this.delayInMilli;
    }

    @Override
    public void setRunnable(final Runnable runnable) {
      synchronized (this.timeoutTask) {
        this.timeoutTask = new Runnable() {
          @Override
          public void run() {
            runnable.run();
          }
        };
      }
    }

    public void setDelay(long delay, TimeUnit unit) {
      this.delayInMilli = unit.toMillis(delay);
    }
  }

