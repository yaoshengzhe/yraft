package com.github.yaoshengzhe.yraft.timer;

public interface TimerService {

  void init(Runnable runnable);

  void reset();

  void stop();
}
