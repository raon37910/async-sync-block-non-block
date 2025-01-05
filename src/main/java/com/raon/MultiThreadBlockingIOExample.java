package com.raon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class MultiThreadBlockingIOExample {
	public static final int PORT = 9999;


	public static void main(String[] args) {
		ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

		try (
			ServerSocket serverSocket = new ServerSocket(PORT)
		) {
			log.info("Server started on port {}", PORT);
			log.info("Waiting for client connection...");
			while (true) {
				Socket socket = serverSocket.accept();

				executorService.submit(() -> {
					try (
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
					) {
						log.info("Client connected {}", socket.getInetAddress());
						// Blocking
						String line = reader.readLine();
						log.info("Received: {}", line);
						writer.println("Server Response");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});

			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
}
