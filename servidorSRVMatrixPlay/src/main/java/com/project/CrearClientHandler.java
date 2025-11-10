package com.project;

import java.util.Map;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

/**
 * Classe encarregada de gestionar la creació i registre de nous clients.
 */
public class CrearClientHandler {

    private final ClientRegistry clients;
    private final Map<String, ClientData> clientsData;
    private final Main server;
    private final ServerUtils serverUtils;

    public CrearClientHandler(ClientRegistry clients, Map<String, ClientData> clientsData,
                              Main server, ServerUtils serverUtils) {
        this.clients = clients;
        this.clientsData = clientsData;
        this.server = server;
        this.serverUtils = serverUtils;
    }

    /**
     * Gestiona la petició d'un client per establir el seu nom.
     * Si el nom ja està en ús, s’envia un missatge d’error.
     * Si és vàlid, s’afegeix el client i se li assigna un color.
     */
    public void handleClientSetName(WebSocket conn, JSONObject obj) {
        String userName = obj.optString("value", "").trim();

        // Comprova que el nom sigui vàlid
        if (userName.isEmpty()) {
            JSONObject error = new JSONObject()
                .put("type", "error")
                .put("value", "Nom no pot estar buit.");
            serverUtils.sendSafe(conn, error.toString());
            conn.close();
            return;
        }

        // Comprova que el nom no estigui en ús
        if (clients.isNameTaken(userName)) {    
            JSONObject error = new JSONObject()
                .put("type", "error")
                .put("value", "Nom ja utilitzat. Tria un altre.");
            serverUtils.sendSafe(conn, error.toString());
            conn.close();

            return;
        }

        clients.add(conn, userName);

        String color = (clients.snapshot().size() == 1) ? "VERMELL" : "NEGRE";

        clientsData.put(userName, new ClientData(userName, color));
        JSONObject ok = new JSONObject()
            .put("type", "RegistreOk")
            .put("value", "Benvingut" + userName + "!");
        //serverUtils.sendSafe(conn, ok.toString());

        System.out.println("Nou client connectat: " + userName + " (" + color + ")");
    }
}
