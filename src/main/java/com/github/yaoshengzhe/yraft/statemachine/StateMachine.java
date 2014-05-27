package com.github.yaoshengzhe.yraft.statemachine;

import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.LogEntry;

public interface StateMachine {

  void commit(LogEntry entry) throws StateMachineException;

}
