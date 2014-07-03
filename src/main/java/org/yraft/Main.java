package org.yraft;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

  public static void main(String[] args) throws SocketException {
    String commitFilePath = "~/tmp/raft";

    Map<Integer, InetSocketAddress> serverInfo = ImmutableMap.<Integer, InetSocketAddress>builder()
            .put(0, new InetSocketAddress("127.0.0.1", 12340))
            .put(1, new InetSocketAddress("127.0.0.1", 12341))
            .put(2, new InetSocketAddress("127.0.0.1", 12342))
            .put(3, new InetSocketAddress("127.0.0.1", 12343))
            .put(4, new InetSocketAddress("127.0.0.1", 12344))
            .build();

    final List<RaftServer> servers = Lists.newArrayList();

    for (int i=0; i < 5; ++i) {
      RaftServer server = Raft.newUDPRaftServer(serverInfo, i, commitFilePath + "_" + i, serverInfo.get(i));
      servers.add(server);
      server.start();
    }

    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        for (RaftServer s : servers) {
          synchronized (s) {
            System.out.println(s);
          }
        }
      }
    }, 0, 10, TimeUnit.SECONDS);
  }
}
