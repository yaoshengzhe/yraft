package com.github.yaoshengzhe.yraft.exception;

public class UnknownRaftMessageException extends Exception {

  public UnknownRaftMessageException() {
    super();
  }

  public UnknownRaftMessageException(String message) {
    super(message);
  }

  public UnknownRaftMessageException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnknownRaftMessageException(Throwable cause) {
    super(cause);
  }
}

