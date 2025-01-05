package com.raon;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NonBlockingIOExample {
	public final static int PORT = 9999;

	@SneakyThrows
	public static void main(String[] args) {
		try (
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()
			){
			// 서버 소켓을 Non Blocking 모드로 설정
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.bind(new InetSocketAddress(PORT));

			log.info("Server started on port {}", PORT);

			// 클라이언트 요청 다중 처리
			Selector selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

			while(true) {
				// Blocking 이벤트 발생 시 까지 대기
				selector.select();

				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = selectedKeys.iterator();

				while(iterator.hasNext()) {
					SelectionKey key = iterator.next();

					// 키를 처리한 후 제거
					iterator.remove();

					if (key.isAcceptable()) {
						ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();
						SocketChannel clientChannel = socketChannel.accept();

						if (clientChannel != null) {
							log.info("Client connected from {}", clientChannel.getRemoteAddress());
							clientChannel.configureBlocking(false); // 클라이언트 소켓도 Non-blocking 모드로 설정
							clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
						}
					} else if (key.isReadable()) {
						SocketChannel clientChannel = (SocketChannel) key.channel();
						ByteBuffer buffer = (ByteBuffer) key.attachment();

						int readBytes = clientChannel.read(buffer);
						if (readBytes == -1) {
							log.info("Client disconnected: {}", clientChannel.getRemoteAddress());
							clientChannel.close();
							return;
						}

						buffer.flip();
						String message = new String(buffer.array(), 0, buffer.limit());
						log.info("Received: {}", message);

						// "exit" 명령 처리
						if (message.trim().equals("exit")) {
							log.info("Server shutting down...");
							clientChannel.close();
							System.exit(0);
						}

						// Echo 응답 보내기
						buffer.clear();
						buffer.put("Server Response\n".getBytes());
						buffer.flip();
						clientChannel.write(buffer);
						buffer.clear();
					}
				}
			}
		}
	}
}
