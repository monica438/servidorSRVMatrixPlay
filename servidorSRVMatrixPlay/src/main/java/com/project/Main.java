package com.project;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class Main extends WebSocketServer {
	
	@Override
	public void onStart() {
		System.out.println("Server started!");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println("Connection closed!");
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println("New connection established!");
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		System.out.println("Error occurred: " + ex.getMessage());
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println("Message received: " + message);
	}
}
