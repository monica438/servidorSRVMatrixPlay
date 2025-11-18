package com.project;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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
    private int bolaVelX;
    private int bolaVelY;
    private final int BOLA_SPEED = 5;
    private final Random rand = new Random();


    private static final String T_COUNTDOWN = "countdown";  
	private final ClientRegistry clients;
    private final CrearClientHandler crearClientHandler;
    private final ServerUtils serverUtils;
    private static final int SEND_FPS = 30;
    private final ScheduledExecutorService ticker;
    private static final int REQUIRED_CLIENTS = 2;
    public static final int WIDTH = 600;
    public static final int HEIGHT = 400;
    public String partida = "";
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
        try {
            GestioDB.crearDB();

            GestioDB.iniciarConnexio();            

        } catch (Exception e) {
            System.err.println("Error de bd: " + e.getMessage());
            e.printStackTrace();
        }

        startTicker();
	}

    private void initializeGameObjects() {
            gameObjects.put("P1", new GameObject("P1", 20, 170, 10, 60, "RED"));   
            gameObjects.put("P2", new GameObject("P2", 570, 200, 10, 60, "BLACK")); 
            gameObjects.put("B0", new GameObject("B0", 295, 195, 10, 10, "WHITE")); 


    }
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        System.out.println("Client desconnectat: " + name);

        // Reiniciar partida solo si falta alguien
        if (clients.snapshot().size() < REQUIRED_CLIENTS) {
            partida = "Esperant";

            GameObject bola = gameObjects.get("B0");
            if (bola != null) {
                bola.x = 295;
                bola.y = 195;
            }

            broadcastStatus();
        }
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
        JSONObject jocData = new JSONObject();
        jocData.put(K_TYPE, "jocData");
        jocData.put("estatPartida", partida);

        JSONArray jugadors = new JSONArray();
        for (String name : clients.snapshot().values()) {
            jugadors.put(name);
        }
        jocData.put("Jugadors", jugadors);

        jocData.put("J1Punts", "0");
        jocData.put("J2Punts", "0");


        JSONArray arrObjects = new JSONArray();
        for (GameObject obj : gameObjects.values()) {
            arrObjects.put(obj.toJSON());
        }
        jocData.put("objectsList", arrObjects);


            broadcast(jocData.toString());
            System.out.println(jocData.toString(4));        
    }
    
    private void handleMove(WebSocket conn, JSONObject obj) {
        String clientName = clients.nameBySocket(conn);
        if (clientName == null) return;

        ClientData cd = clientsData.get(clientName);
        if (cd == null) return;

        // Determinar qué pala mover según el color del jugador
        String objectName = cd.color.equals("VERMELL") ? "P1" : "P2";
        GameObject paddle = gameObjects.get(objectName);
        if (paddle == null) return;

        int speed = 8;
        String dir = obj.optString("direction");

        switch (dir) {
            case "up":
                paddle.y -= speed;
                break;
            case "down":
                paddle.y += speed;
                break;
        }

        // Limitar el movimiento dentro de la ventana
        if (paddle.y < 0) {
            paddle.y = 0;
        }
        // Aquí nos aseguramos de que no se salga por abajo
        int margin = 60;
        if (paddle.y > HEIGHT - paddle.alto - margin) {
            paddle.y = HEIGHT - paddle.alto - margin;
        }

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

                    if (clients.snapshot().size() < REQUIRED_CLIENTS) {
                        break;
                    }

                    sendCountdownToAll(i);
                    if (i == 0) {
                        Thread.sleep(250); 
                        partida = "Jugant";
                        GameObject bola = gameObjects.get("B0");
                        if (bola != null) {
                            bola.x = 295; 
                            bola.y = 195;
                            //bolaVelX = rand.nextBoolean() ? BOLA_SPEED : -BOLA_SPEED;
                            //bolaVelY = rand.nextInt(5) - 2;
                        }
                        broadcastStatus(); 
                    } else {
                        Thread.sleep(750);
                    }
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
                        config.put("url", "wss://matrixplay4.ieti.site:443");
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
                case "move":
                    handleMove(conn, obj);
                    break;

                case "partida":
                    String nuevoValor = obj.optString("value", "");
                    if (partida.equals("Jugant") && nuevoValor.equals("Esperant")) {
                        break;
                    }

                    partida = nuevoValor;
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
                /*GameObject bola = gameObjects.get("B0");
                if (bola != null) {
                    // Mover la bola
                    bola.x += bolaVelX;
                    bola.y += bolaVelY;

                    // Rebotar arriba y abajo
                    if (bola.y <= 0) {
                        bola.y = 0;
                        bolaVelY *= -1;
                    }
                    if (bola.y >= 400 - bola.alto) { // altura del canvas = 400
                        bola.y = 400 - bola.alto;
                        bolaVelY *= -1;
                    }
                    if (bola.x <= 0) {
                        bola.x = 0;
                        bolaVelX *= -1;
                    }
                    if (bola.x >= 600 - bola.ancho) { // ancho del canvas = 600
                        bola.x = 600 - bola.ancho;
                        bolaVelX *= -1;
                    }

                    // Colisión con paletas
                    GameObject p1 = gameObjects.get("P1");
                    GameObject p2 = gameObjects.get("P2");
                    if ((p1 != null && colision(bola, p1)) || (p2 != null && colision(bola, p2))) {
                        bolaVelX *= -1;
                    }
                }*/







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

    private boolean colision(GameObject bola, GameObject paddle) {
        return bola.x < paddle.x + paddle.ancho &&
            bola.x + bola.alto > paddle.x &&
            bola.y < paddle.y + paddle.alto &&
            bola.y + bola.alto > paddle.y;
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
