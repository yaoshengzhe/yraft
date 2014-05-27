package com.github.yaoshengzhe.yraft.statemachine;

import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.LogEntry;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

public class LocalDiskStateMachine implements StateMachine {

  final File commitFile;

  public LocalDiskStateMachine(File commitFile) {
    this.commitFile = commitFile;
  }

  @Override
  public void commit(LogEntry entry) throws StateMachineException {
    try {
      Files.asByteSink(this.commitFile, FileWriteMode.APPEND).write(entry.toByteArray());
    } catch (IOException e) {
      throw new StateMachineException(e);
    }
  }
}
