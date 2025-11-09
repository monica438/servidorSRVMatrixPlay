package com.project;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import org.w3c.dom.html.HTMLOListElement;

public class Main extends WebSocketServer {

    public static final int DEFAULT_PORT = 3000;

	private final Map<String, ClientData> clientsData = new HashMap<>();

	private final ClientRegistry clients;
    private final CrearClientHandler crearClientHandler;
    private final ServerUtils serverUtils;

	
    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry();
        serverUtils = new ServerUtils(this, clients, clientsData);

        crearClientHandler = new CrearClientHandler(clients,clientsData,this,serverUtils);
    }
	@Override
	public void onStart() {
		System.out.println("Server started!");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        System.out.println("Client desconnectat: " + name);
    }

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println("New connection established!");
		JSONObject hola = new JSONObject();
		hola.put("type", "broadcastHola");
		hola.put("value", "hola");
		broadcast(hola.toString());
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		System.out.println("Error occurred: " + ex.getMessage());
	}

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject obj;
        try {
            obj = new JSONObject(message);
        } catch (Exception ex) {
            return; // JSON invàlid
        }
        String type = obj.optString("type", "");
        switch (type) {
            case "setName":
                 crearClientHandler.handleClientSetName(conn, obj);
                
                break;
        
            default:
                break;
        }
    
    
    }
	    private static void registerShutdownHook(Main server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Aturant servidor...");
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servidor aturat.");
        }));
    }

    private static void awaitForever() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

	    /** Punt d'entrada. */
    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        registerShutdownHook(server);

        System.out.println("Servidor executant-se al port " + DEFAULT_PORT + ". Prem Ctrl+C per aturar-lo.");
        awaitForever();
    }
}
