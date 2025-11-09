package com.project;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
public class ServerUtils {
    

    private final Main server;
    private final ClientRegistry clients;
    private final Map<String, ClientData> clientsData;

    public ServerUtils(Main server,ClientRegistry clients, Map<String,ClientData> clientsData) {
        this.server = server;
        this.clients = clients;
        this.clientsData = clientsData;

    }

    
    /** Envia de forma segura un payload i, si el socket no està connectat, el neteja del registre. */
    public void sendSafe(WebSocket to, String payload) {
        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            String name = clients.cleanupDisconnected(to);
            clientsData.remove(name);
            System.out.println("Client desconnectat durant send: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    
}
