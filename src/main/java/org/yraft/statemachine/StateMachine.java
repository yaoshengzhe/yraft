package org.yraft.statemachine;

import org.yraft.protobuf.generated.RaftProtos.LogEntry;

public interface StateMachine {

  void commit(LogEntry entry) throws StateMachineException;

}
