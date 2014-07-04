package org.yraft.network;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yraft.Messages;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

public class UDPCommunicator extends AbstractCommunicator {

  private static final Logger LOG = LoggerFactory.getLogger(UDPCommunicator.class);

  private Map<Integer, InetSocketAddress> serverTable = Collections.EMPTY_MAP;
  private final DatagramSocket socket;
  private final ByteBuffer buf;
  private static final int MAX_BUF_SIZE = 8192;

  public UDPCommunicator(SocketAddress addr) throws SocketException {
    this.socket = new DatagramSocket(addr);
    this.buf = ByteBuffer.allocate(MAX_BUF_SIZE);
  }

  @Override
  public void setMembers(Map<Integer, InetSocketAddress> servers) {
    serverTable = Maps.newHashMap(servers);
  }

  @Override
  public void broadcast(Messages msg, byte[] data) {

    ByteBuffer packet = ByteBuffer.allocate(4 + data.length);
    packet.putInt(msg.getId());
    packet.put(data);

    for (InetSocketAddress addr : this.serverTable.values()) {
      sendTo(addr, packet.array());
    }
  }

  @Override
  public void sendTo(int candidateId, Messages msg, byte[] data) {

    // LOG.debug("Send message from " + this.server.getCandidateId() + " to " + candidateId);
    InetSocketAddress addr = this.serverTable.get(candidateId);
    if (addr != null) {
      sendTo(addr, msg, data);
    }
  }

  @Override
  public void run() {
    while (true) {
      DatagramPacket request = new DatagramPacket(this.buf.array(), this.buf.capacity());
      try {
        socket.receive(request);
        ByteBuffer buf = ByteBuffer.wrap(request.getData(), 0, request.getLength());
        Messages msg = Messages.fromId(buf.getInt());
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        server.receive(msg, data);

      } catch (IOException e) {
        LOG.error("", e);
      } catch (Exception e) {
        LOG.error("", e);
      }
    }
  }

  public void sendTo(SocketAddress addr, Messages msg, byte[] data) {

    ByteBuffer packet = ByteBuffer.allocate(4 + data.length);
      packet.putInt(msg.getId());
      packet.put(data);
    sendTo(addr, packet.array());
  }

  public void sendTo(SocketAddress addr, byte[] data) {

    try {
      this.socket.send(new DatagramPacket(data, data.length, addr));
    } catch (IOException e) {
      LOG.error(e.toString());
    }
  }
}
