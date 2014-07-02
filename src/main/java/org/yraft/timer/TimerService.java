package org.yraft.timer;

public interface TimerService {

  void reset();

  void start();

  void stop();

  long getRecentTimeoutInMills();

  void setRunnable(final Runnable runnable);
}
