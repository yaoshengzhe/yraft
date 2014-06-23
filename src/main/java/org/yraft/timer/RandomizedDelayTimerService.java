package org.yraft.timer;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class RandomizedDelayTimerService implements TimerService {

  private final ScheduledDelayTimerService delegate;
  private final int delayFactor;
  private final long delayInMilli;
  private final Random rand;

  public RandomizedDelayTimerService(long delay, TimeUnit unit, final Runnable runnable, int delayFactor) {
    checkArgument(delayFactor >= 0 && delayFactor <= 100, "delayFactor should be in range [0, 100]");
    this.delegate = new ScheduledDelayTimerService(delay, unit, runnable);
    this.delayInMilli = unit.toMillis(delay);
    this.delayFactor = delayFactor;
    this.rand = new Random();
  }

  @Override
  public void reset() {
    long delayedMillis = (long)((this.rand.nextInt(this.delayFactor) / 100.0) * this.delayInMilli);
    this.delegate.setDelay(this.delayInMilli + delayedMillis, TimeUnit.MILLISECONDS);
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
}
