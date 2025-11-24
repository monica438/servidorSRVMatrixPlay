package com.project;

import java.sql.SQLException;
import java.time.LocalDate;
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
        System.out.println("[handleClientSetName] from " + conn.getRemoteSocketAddress() + " payload=" + obj.toString());
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
            WebSocket oldConn = clients.socketByName(userName);
            if (oldConn != null) {
                System.out.println("[handleClientSetName] Usuario " + userName + " ya existe. Reemplazando sesión anterior.");
                // Cerrar conexión anterior si está abierta
                if (oldConn.isOpen()) {
                    oldConn.close(1000, "Sesión reemplazada por nueva conexión");
                }
                // Eliminar del registro inmediatamente para permitir el nuevo registro
                clients.remove(oldConn);
                // IMPORTANTE: Eliminar también de clientsData para liberar el color
                clientsData.remove(userName);
            }
        }

        // Verificar si la partida está llena (máximo 2 jugadores)
        if (clients.snapshot().size() >= 2) {
            JSONObject error = new JSONObject()
                .put("type", "error")
                .put("value", "Partida plena. Espera a que acabi.");
            serverUtils.sendSafe(conn, error.toString());
            conn.close();
            return;
        }

        clients.add(conn, userName);

        // Asignar color basado en disponibilidad (orden de entrada/huecos libres)
        String color = "RED";
        boolean redTaken = false;
        for (ClientData cd : clientsData.values()) {
            if ("RED".equals(cd.color)) {
                redTaken = true;
                break;
            }
        }
        
        if (redTaken) {
            color = "BLACK";
        }
        
        System.out.println("[handleClientSetName] Asignando color " + color + " a " + userName);

        clientsData.put(userName, new ClientData(userName, color));
        JSONObject ok = new JSONObject()
            .put("type", "RegistreOk")
            .put("value", "Benvingut " + userName + "!")
            .put("color", color)
            .put("playerNumber", clients.snapshot().size()); 
        serverUtils.sendSafe(conn, ok.toString());
        try {
            GestioDB.afegeixEntradaLog("Nou client connectat: " + userName + " (" + color + ")",LocalDate.now().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (clients.snapshot().size() == 2) {  // REQUIRED_CLIENTS
        server.sendCountdown();
    }
    }
}
