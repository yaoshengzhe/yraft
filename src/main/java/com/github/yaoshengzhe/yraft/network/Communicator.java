package com.github.yaoshengzhe.yraft.network;

import com.github.yaoshengzhe.yraft.Messages;

public interface Communicator {

  void send(Messages msg, byte[] data);

}
