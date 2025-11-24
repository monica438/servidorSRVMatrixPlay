package com.project;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
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

	private final Map<String, ClientData> clientsData = new ConcurrentHashMap<>();
    private final Map<String, GameObject> gameObjects = new ConcurrentHashMap<>();
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
    public int J1punts = 0;
    public int J2Punts = 0;
    public GameObject ultimJugadorGol = null;

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
            gameObjects.put("P1", new GameObject("P1", 20, 170, 15, 100, "RED"));   
            gameObjects.put("P2", new GameObject("P2", 570, 200, 15, 100, "BLACK")); 
            gameObjects.put("B0", new GameObject("B0", 295, 195, 22, 22, "WHITE")); 


    }
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        clientsData.remove(name);
        System.out.println("Client desconnectat: " + name);

        // Reiniciar la partida siempre que un cliente se desconecte
        reiniciarPartida();

        // Opcional: si quieres iniciar la cuenta atrás solo cuando vuelvan suficientes jugadores
        if (clients.snapshot().size() == REQUIRED_CLIENTS) {
            sendCountdown();
        }
    }



	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println("New connection established!");
        try {
            GestioDB.afegeixEntradaLog("Raspberry connectada",LocalDate.now().toString());
            
        } catch (Exception e) {
            // TODO: handle exception
        }
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
            ClientData cd = clientsData.get(name);
            if (cd != null) {
                // Sincronizar datos del objeto de juego con ClientData antes de enviar
                String objectName = cd.color.equals("RED") ? "P1" : "P2";
                GameObject paddle = gameObjects.get(objectName);
                if (paddle != null) {
                    cd.palaX = paddle.x;
                    cd.palaY = paddle.y;
                }
                cd.punts = cd.color.equals("RED") ? J1punts : J2Punts;
                jugadors.put(cd.toJSON());
            }
        }
        jocData.put("Jugadors", jugadors);

        jocData.put("J1Punts", J1punts);
        jocData.put("J2Punts", J2Punts);


        JSONArray arrObjects = new JSONArray();
        for (GameObject obj : gameObjects.values()) {
            arrObjects.put(obj.toJSON());
        }
        jocData.put("objectsList", arrObjects);


            broadcast(jocData.toString());
            // System.out.println(jocData.toString(4)); // Reducir logs
    }
    
    private void handleMove(WebSocket conn, JSONObject obj) {
        String clientName = clients.nameBySocket(conn);
        // System.out.println("[handleMove] clientName=" + clientName + " direction=" + obj.optString("direction"));
        if (clientName == null) return;

        ClientData cd = clientsData.get(clientName);
        if (cd == null) return;

        String objectName = cd.color.equals("RED") ? "P1" : "P2";
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

        if (paddle.y < 0) {
            paddle.y = 0;
        }
        if (paddle.y > HEIGHT - paddle.alto) {
            paddle.y = HEIGHT - paddle.alto;
        }

        // System.out.println("[handleMove] " + objectName + " moved to y=" + paddle.y);

    }

    private void handlePosition(WebSocket conn, JSONObject obj) {
        String clientName = clients.nameBySocket(conn);
        int y = obj.optInt("y", -1);
        // System.out.println("[handlePosition] clientName=" + clientName + " y=" + y);
        
        if (clientName == null || y < 0) return;

        ClientData cd = clientsData.get(clientName);
        if (cd == null) return;

        // Determinar qué pala mover según el color del jugador
        String objectName = cd.color.equals("RED") ? "P1" : "P2";
        GameObject paddle = gameObjects.get(objectName);
        if (paddle == null) return;

        // Actualizar posición directamente
        paddle.y = y;

        // Limitar a los bordes del canvas
        if (paddle.y < 0) {
            paddle.y = 0;
        }
        if (paddle.y > HEIGHT - paddle.alto) {
            paddle.y = HEIGHT - paddle.alto;
        }
        
        // System.out.println("[handlePosition] " + objectName + " set to y=" + paddle.y);
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
                            bola.x = WIDTH / 2 -bola.ancho / 2; 
                            bola.y = rand.nextBoolean() ? HEIGHT / 4 : 3 * HEIGHT / 4;

                            bolaVelX = rand.nextBoolean() ? BOLA_SPEED : -BOLA_SPEED;
                            bolaVelY = rand.nextInt(3) - 1; 
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
        System.out.println("[onMessage] From " + conn.getRemoteSocketAddress() + " raw: " + message);
        JSONObject obj;
        try {
            obj = new JSONObject(message);
            System.out.println("[onMessage] Parsed JSON type=" + obj.optString("type"));
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
                case "position":
                    handlePosition(conn, obj);
                    break;

                case "partida":
                    String nuevoValor = obj.optString("value", "");
                    if (partida.equals("Jugant") && nuevoValor.equals("Esperant")) {
                        break;
                    }

                    partida = nuevoValor;
                    break;
                case "desconecta":
                    clients.remove(conn);
                    conn.close(1000,"Fi de partida");
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
                    if (partida.equals("Finalitzada")){
                        return;
                    }
                    GameObject bola = gameObjects.get("B0");
                    GameObject p1 = gameObjects.get("P1");
                    GameObject p2 = gameObjects.get("P2");

                    if (bola != null) {
                        float nextX = bola.x + bolaVelX;
                        float nextY = bola.y + bolaVelY;

                        // Colisión con P1
                        if (p1 != null) {
                            float[] hitP1 = ballIntersectsPaddle(
                                bola.x, bola.y,
                                nextX, nextY,
                                p1.x, p1.y,
                                p1.x, p1.y + p1.alto
                            );
                            if (hitP1 != null) {
                                bolaVelX *= -1;
                                float paddleCenter = p1.y + p1.alto / 2f;
                                float relativeIntersectY = bola.y + bola.alto / 2f - paddleCenter;
                                float normalizedRelativeIntersectionY = relativeIntersectY / (p1.alto / 2f);
                                bolaVelY = (int)(normalizedRelativeIntersectionY * BOLA_SPEED);
                                bola.x = (int) hitP1[0];
                                bola.y = (int) hitP1[1];
                                nextX = bola.x + bolaVelX;
                                nextY = bola.y + bolaVelY;
                            }
                        }

                        if (p2 != null) {
                            float[] hitP2 = ballIntersectsPaddle(
                                bola.x, bola.y,
                                nextX, nextY,
                                p2.x, p2.y,
                                p2.x, p2.y + p2.alto
                            );
                            if (hitP2 != null) {
                                bolaVelX *= -1;
                                float paddleCenter = p1.y + p1.alto / 2f;
                                float relativeIntersectY = bola.y + bola.alto / 2f - paddleCenter;
                                float normalizedRelativeIntersectionY = relativeIntersectY / (p1.alto / 2f);
                                bolaVelY = (int)(normalizedRelativeIntersectionY * BOLA_SPEED);
                                bola.x = (int) hitP2[0];
                                bola.y = (int) hitP2[1];
                                nextX = bola.x + bolaVelX;
                                nextY = bola.y + bolaVelY;
                            }
                        }

                        bola.x += bolaVelX;
                        bola.y += bolaVelY;

                        if (bola.y <= 0) {
                            bola.y = 0;
                            bolaVelY *= -1;
                        }
                            
                        if (bola.y >= HEIGHT - bola.alto) {
                            bola.y = HEIGHT - bola.alto;
                            bolaVelY *= -1;
                        }

                        
                        if (bola.x <= 0) {
                            bola.x = 0;
                            J2Punts++;
                            ultimJugadorGol = p1;
                            reiniciarBola();
                        }
                        if (bola.x >= 600 - bola.ancho) {
                            bola.x = 600 - bola.ancho;
                            J1punts++;
                            ultimJugadorGol = p2;
                            reiniciarBola();
                            }
                        gestionarGols();
                    }

                    broadcastStatus();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }




    private void gestionarGols() {
        if (J1punts < 3 && J2Punts < 3) return;

        partida = "Finalitzada";

        String guanyador = "";
        String perdedor = "";
        String colorWinner = "";
        String colorLoser = "";

        if (ultimJugadorGol == gameObjects.get("P1")) {
            guanyador = clientsData.entrySet().stream()
                    .filter(e -> e.getValue().color.equals("RED"))
                    .map(Map.Entry::getKey).findFirst().orElse("Desconegut");
            perdedor = clientsData.entrySet().stream()
                    .filter(e -> e.getValue().color.equals("BLACK"))
                    .map(Map.Entry::getKey).findFirst().orElse("Desconegut");
            colorWinner = "RED";
            colorLoser = "BLACK";
        } 
        else if (ultimJugadorGol == gameObjects.get("P2")) {
            guanyador = clientsData.entrySet().stream()
                    .filter(e -> e.getValue().color.equals("BLACK"))
                    .map(Map.Entry::getKey).findFirst().orElse("Desconegut");
            perdedor = clientsData.entrySet().stream()
                    .filter(e -> e.getValue().color.equals("RED"))
                    .map(Map.Entry::getKey).findFirst().orElse("Desconegut");
            colorWinner = "BLACK";
            colorLoser = "RED";
        }

        // --- Enviar GameOver ---
        JSONObject msg = new JSONObject();
        msg.put("type", "gameOver");
        msg.put("winner", guanyador);
        msg.put("colorWinner", colorWinner);
        msg.put("loser", perdedor);
        msg.put("colorLoser", colorLoser);
        broadcast(msg.toString());

        System.out.println("Partida finalitzada! Guanyador: " + guanyador);

        // Desconectar a todos los jugadores al finalizar la partida
        for (WebSocket ws : clients.snapshot().keySet()) {
             ws.close(1000, "Partida finalitzada");
        }

        // --- Reset COMPLETO ---
        reiniciarPartida();
    }


    private void reiniciarBola() {
        GameObject bola = gameObjects.get("B0");

        bola.x = WIDTH / 2 - bola.ancho / 2;
        bola.y = rand.nextBoolean() ? HEIGHT / 4 : (3 * HEIGHT / 4);

        bolaVelX = rand.nextBoolean() ? BOLA_SPEED : -BOLA_SPEED;
        bolaVelY = rand.nextInt(3) - 1;
    }

    

    private void stopTicker() {
        try {
            ticker.shutdownNow();
            ticker.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }


    public static float[] ballIntersectsPaddle(
            float ballPosX, float ballPosY,
            float ballNextX, float ballNextY,
            float paddleBottomX, float paddleBottomY,
            float paddleTopX, float paddleTopY
    ) {
        // Line AB = ball movement
        float x1 = ballPosX;
        float y1 = ballPosY;
        float x2 = ballNextX;
        float y2 = ballNextY;

        // Line CD = paddle segment
        float x3 = paddleBottomX;
        float y3 = paddleBottomY;
        float x4 = paddleTopX;
        float y4 = paddleTopY;

        // Compute denominador for intersection
        float denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

        // Parallel or collinear → no intersection
        if (denom == 0) {
            return null;
        }

        // Compute t and u for parametric intersection
        float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        float u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;

        // Check if intersection lies within both segments
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            float ix = x1 + t * (x2 - x1);
            float iy = y1 + t * (y2 - y1);
            return new float[] { ix, iy };
        }

        return null;
    }



    public void reiniciarPartida() {
        // Reset puntuaciones
        J1punts = 0;
        J2Punts = 0;

        // Reset jugador del último gol
        ultimJugadorGol = null;

        // Reset palas
        GameObject p1 = gameObjects.get("P1");
        if (p1 != null) {
            p1.x = 20;
            p1.y = 170;
        }

        GameObject p2 = gameObjects.get("P2");
        if (p2 != null) {
            p2.x = 570;
            p2.y = 200;
        }

        // Reset bola
        reiniciarBola();

        // Volver a estado inicial
        partida = "Esperant";

        // Actualizar a clientes
        broadcastStatus();
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
