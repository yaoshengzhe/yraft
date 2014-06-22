package org.yraft.timer;

import java.util.concurrent.TimeUnit;

public interface TimerService {

  void reset();

  void start();

  void stop();

  void setDelay(long delay, TimeUnit unit);

}
