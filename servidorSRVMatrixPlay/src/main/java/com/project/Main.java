package com.project;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main extends WebSocketServer {

    public static final int DEFAULT_PORT = 3000;

	private final Map<String, ClientData> clientsData = new HashMap<>();
    private final Map<String, GameObject> gameObjects = new HashMap<>();
    private volatile boolean countdownRunning = false;
    private static final String K_TYPE = "type";
    private static final String K_VALUE = "value";


    private static final String T_COUNTDOWN = "countdown";  
	private final ClientRegistry clients;
    private final CrearClientHandler crearClientHandler;
    private final ServerUtils serverUtils;
    private static final int SEND_FPS = 30;
    private final ScheduledExecutorService ticker;
    private static final int REQUIRED_CLIENTS = 2;

    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry();
        serverUtils = new ServerUtils(this, clients, clientsData);

        crearClientHandler = new CrearClientHandler(clients,clientsData,this,serverUtils);
            ThreadFactory tf = r -> {
            Thread t = new Thread(r, "ServerTicker");
            t.setDaemon(true);
            return t;
        };
        this.ticker = Executors.newSingleThreadScheduledExecutor(tf);
        initializeGameObjects();

    }
	@Override
	public void onStart() {
		System.out.println("Server started!");
        startTicker();
	}

    private void initializeGameObjects() {
    // Pala del Jugador 1
    gameObjects.put("P1", new GameObject("P1", 0, 3, 1, 3,"RED")); 

    // Pala del Jugador 2
    gameObjects.put("P2", new GameObject("P2", 7, 3, 1, 3,"BLACK")); 

    // Pelota
    gameObjects.put("B0", new GameObject("B0", 4, 4, 1, 1,"WHITE"));
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
		hola.put(K_TYPE, "broadcastHola");
		hola.put(K_VALUE, "hola");
		broadcast(hola.toString());
        sendCountdown();

	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		System.out.println("Error occurred: " + ex.getMessage());
	}

    private void broadcastStatus() {
        // --- Estado general de la partida ---
        JSONObject jocData = new JSONObject();
        jocData.put(K_TYPE, "jocData");
        jocData.put("estatPartida", "Jugant");

        JSONArray jugadors = new JSONArray();
        for (String name : clients.snapshot().values()) {
            jugadors.put(name);
        }
        jocData.put("Jugadors", jugadors);

        jocData.put("J1Punts", "0");
        jocData.put("J2Punts", "0");

        // --- Tablero (ejemplo base 8x8) ---
        int[][] board = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 0, 0},
            {0, 1, 3, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 2, 0},
            {0, 0, 0, 0, 0, 0, 2, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0}
        };
        jocData.put("board", board);

        // --- Objetos del juego (palas y pelota) ---
        JSONArray arrObjects = new JSONArray();
        for (GameObject obj : gameObjects.values()) {
            arrObjects.put(obj.toJSON());
        }
        jocData.put("objectsList", arrObjects);

        // --- Enviar a todos los clientes ---
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            WebSocket conn = e.getKey();
            serverUtils.sendSafe(conn, jocData.toString());
            System.out.println(jocData.toString(4));        }
    }







     public void sendCountdown() {
        synchronized (this) {
            if (countdownRunning) return;
            if (clients.snapshot().size() != REQUIRED_CLIENTS) return;
            countdownRunning = true;
        }

        new Thread(() -> {
            try {
                for (int i = 3; i >= 0; i--) {
                    // Si durant el compte enrere ja no hi ha els clients requerits, cancel·la
                    if (clients.snapshot().size() < REQUIRED_CLIENTS) {
                        break;
                    }

                    sendCountdownToAll(i);
                    if (i > 0) Thread.sleep(750); // ritme del compte enrere
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                countdownRunning = false;
            }
        }, "CountdownThread").start();
    }

        private static JSONObject msg(String type) {
        return new JSONObject().put(K_TYPE, type);
    }

    private void sendCountdownToAll(int n) {
        JSONObject rst = msg(T_COUNTDOWN).put(K_VALUE, n);
        broadcast(rst.toString());
    }
        private void broadcastExcept(WebSocket sender, String payload) {
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            WebSocket conn = e.getKey();
            if (!Objects.equals(conn, sender)) serverUtils.sendSafe(conn, payload);
        }
    }


    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject obj;
        try {
            obj = new JSONObject(message);
        } catch (Exception ex) {
            return; // JSON invàlid
        }
        String type = obj.optString(K_TYPE, "");
        switch (type) {
                case "setName":
                    crearClientHandler.handleClientSetName(conn, obj);
                    break;
                case "raspberry":
                    String requestMessage = obj.optString("message", "");
                    if ("solicito_config".equals(requestMessage)) {
                        JSONObject config = new JSONObject();
                        config.put(K_TYPE, "config");
                        config.put("groupName", "Grup4");
                        serverUtils.sendSafe(conn, config.toString());
                        System.out.println("[server] Enviado nombre del grupo a la Raspberry");
                    } else {
                        JSONObject respuesta = new JSONObject();
                        respuesta.put(K_TYPE, "ack_raspberry");
                        respuesta.put("message", "Hola Raspberry! He recibido tu mensaje correctamente.");
                        respuesta.put("status", "connected");
                        serverUtils.sendSafe(conn, respuesta.toString());
                    }
                    break;

                default:
                    break;
        }   
    }
	    private static void registerShutdownHook(Main server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Aturant servidor...");
            try {
                server.stopTicker();  
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

        private void startTicker() {
        long periodMs = Math.max(1, 1000 / SEND_FPS);
        ticker.scheduleAtFixedRate(() -> {
            try {
                if (!clients.snapshot().isEmpty()) {
                    broadcastStatus();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    private void stopTicker() {
        try {
            ticker.shutdownNow();
            ticker.awaitTermination(1, TimeUnit.SECONDS);
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
