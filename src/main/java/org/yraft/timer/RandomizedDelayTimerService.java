package org.yraft.timer;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class RandomizedDelayTimerService implements TimerService {

  private final ScheduledDelayTimerService delegate;
  private final int delayFactor;
  private final long delayInMilli;
  private long recentTimeoutInMills;
  private final Random rand;

  public RandomizedDelayTimerService(long delay, TimeUnit unit, int delayFactor) {
    checkArgument(delayFactor >= 0 && delayFactor <= 100, "delayFactor should be in range [0, 100]");
    this.delegate = new ScheduledDelayTimerService(delay, unit);
    this.delayInMilli = unit.toMillis(delay);
    this.delayFactor = delayFactor;
    this.rand = new Random();
  }

  @Override
  public void reset() {
    long delayedMillis = (long)((this.rand.nextInt(this.delayFactor) / 100.0) * this.delayInMilli);
    this.recentTimeoutInMills = this.delayInMilli + delayedMillis;
    this.delegate.setDelay(this.recentTimeoutInMills, TimeUnit.MILLISECONDS);
    this.delegate.reset();
  }

  @Override
  public void start() {
    this.delegate.start();
  }

  @Override
  public void stop() {
    this.delegate.stop();
  }

  @Override
  public long getRecentTimeoutInMills() {
    return this.recentTimeoutInMills;
  }

  @Override
  public void setRunnable(final Runnable runnable) {
    this.delegate.setRunnable(runnable);
  }
}
