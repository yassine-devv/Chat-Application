package com.example.chat_app.config;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketConnectionsTracker {

    //uso concurrenthashmap perché è thread-safe e perfetto per concurrent WebSocket eventi
    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();

    public Set<String> getConnectedUsers() {
        return connectedUsers;
    }

    public void addUser(String username) {
        connectedUsers.add(username);
    }

    public void removeUser(String username) {
        connectedUsers.remove(username);
    }
}
