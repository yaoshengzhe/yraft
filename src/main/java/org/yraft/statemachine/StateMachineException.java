package org.yraft.statemachine;

public class StateMachineException extends Exception {

  public StateMachineException() {
    super();
  }

  public StateMachineException(String message) {
    super(message);
  }

  public StateMachineException(String message, Throwable cause) {
    super(message, cause);
  }

  public StateMachineException(Throwable cause) {
    super(cause);
  }
}
