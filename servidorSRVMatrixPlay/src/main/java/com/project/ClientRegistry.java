package com.project;

import org.java_websocket.WebSocket;
import org.json.JSONArray;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Collections;

/**
 * Registre de clients connectats usando nombres reales de usuarios.
 */
final class ClientRegistry {

    /** Mapa de sockets a nombres de usuario (orden de inserción garantizado). */
    private final Map<WebSocket, String> bySocket =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /** Mapa de nombres de usuario a sockets (orden de inserción garantizado). */
    private final Map<String, WebSocket> byName =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Constructor simple.
     */
    ClientRegistry() {
    }

    /**
     * Añade un nuevo cliente con su nombre
     */
    String add(WebSocket socket, String userName) {
        synchronized (byName) {
            bySocket.put(socket, userName);
            byName.put(userName, socket);
        }
        return userName;
    }

    /**
     * Verifica si un nombre ya está en uso
     */
    boolean isNameTaken(String userName) {
        synchronized (byName) {
            return byName.containsKey(userName);
        }
    }

    /**
     * Elimina un cliente del registro
     */
    String remove(WebSocket socket) {
        synchronized (byName) {
            String userName = bySocket.remove(socket);
            if (userName != null) {
                byName.remove(userName);
            }
            return userName;
        }
    }

    /**
     * Obtiene el socket asociado a un nombre de usuario
     */
    WebSocket socketByName(String userName) {
        synchronized (byName) {
            return byName.get(userName);
        }
    }

    /**
     * Obtiene el nombre asociado a un socket
     */
    String nameBySocket(WebSocket socket) {
        synchronized (byName) {
            return bySocket.get(socket);
        }
    }

    /**
     * Devuelve el conjunto de nombres de usuarios conectados (ordenado por llegada)
     */
    public Set<String> getAllUserNames() {
        synchronized (byName) {
            return Set.copyOf(byName.keySet());
        }
    }

    /**
     * Retorna la lista actual de nombres de usuarios conectados como JSONArray
     */
    JSONArray currentNames() {
        JSONArray arr = new JSONArray();
        synchronized (byName) {
            for (String userName : byName.keySet()) {
                arr.put(userName);
            }
        }
        return arr;
    }

    /**
     * Limpia un socket desconectado
     */
    String cleanupDisconnected(WebSocket socket) {
        return remove(socket);
    }

    /**
     * Retorna una copia del mapa actual de sockets → nombres
     * (con orden de inserción garantizado)
     */
    Map<WebSocket, String> snapshot() {
        synchronized (byName) {
            return Map.copyOf(bySocket);
        }
    }
}
