package org.yraft.statemachine;

import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import org.yraft.protobuf.generated.RaftProtos.LogEntry;

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
