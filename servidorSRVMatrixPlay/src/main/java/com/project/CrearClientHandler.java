package com.project;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    public CrearClientHandler(ClientRegistry clients, Map<String, ClientData> clientsData,
                              Main server, ServerUtils serverUtils) {
        this.clients = clients;
        this.clientsData = clientsData;
        this.server = server;
        this.serverUtils = serverUtils;
    }
    public String horaActual() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Gestiona la petició d'un client per establir el seu nom.
     * Si el nom ja està en ús, s’envia un missatge d’error.
     * Si és vàlid, s’afegeix el client i se li assigna un color.
     */
    public void handleClientSetName(WebSocket conn, JSONObject obj) {
        System.out.println("[handleClientSetName] from " + conn.getRemoteSocketAddress() + " payload=" + obj.toString());
        String userName = obj.optString("value", "").trim();

        if (userName.isEmpty()) {
            JSONObject error = new JSONObject()
                .put("type", "error")
                .put("value", "Nom no pot estar buit.");
            serverUtils.sendSafe(conn, error.toString());
            conn.close();
            return;
        }

        if (clients.isNameTaken(userName)) {
            JSONObject error = new JSONObject()
                .put("type", "error")
                .put("value", "Nom ja utilitzat. Tria un altre.");
            serverUtils.sendSafe(conn, error.toString());
            conn.close();
            return;
        }

        clients.add(conn, userName);
        boolean existeixVermell = clientsData.values().stream()
                .anyMatch(d -> d.getColor().equals("VERMELL"));

        String color = existeixVermell ? "NEGRE" : "VERMELL";

        clientsData.put(userName, new ClientData(userName, color));

        JSONObject ok = new JSONObject()
            .put("type", "RegistreOk")
            .put("value", "Benvingut " + userName + "!")
            .put("color", color)
            .put("playerNumber", clients.snapshot().size());
        serverUtils.sendSafe(conn, ok.toString());

        try {
            GestioDB.afegeixEntradaLog("Nou client connectat: " + userName + " (" + color + ")", horaActual());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (clients.snapshot().size() == 2) {
            server.sendCountdown();
        }
    }

}
