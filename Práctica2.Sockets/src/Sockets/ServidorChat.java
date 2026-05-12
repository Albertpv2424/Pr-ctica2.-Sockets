package Sockets;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorChat {

	static int comptadorClients = 0; // Número que se asigna a cada cliente
	static int clientsActius = 0; // Número de clientes conectados
	static boolean haTingutClients = false; // Indica si alguna vez hubo clientes
	static boolean servidorTancant = false; // Indica si el servidor se está cerrando
	static ArrayList<ClientHandler> handlersActius = new ArrayList<>(); // Lista de handlers activos
	static ArrayList<String[]> coadeMissatges = new ArrayList<>(); // Cola FIFO de mensajes
	static String paraulaClauServidor; // Palabra clave del servidor
	static ServerSocket serverSocket; // Socket servidor
	static Scanner teclat = new Scanner(System.in);

	public static void main(String[] args) {

		// puerto + palabra clave + máximo clientes
		if (args.length < 3) {
			System.out.println("Ús: java ServidorChat <port> <paraula_clau> <max_clients>");
			return;
		}

		int port = Integer.parseInt(args[0]);
		paraulaClauServidor = args[1];
		int maxClients = Integer.parseInt(args[2]);
		System.out.println("PORT_SERVIDOR: " + port);
		System.out.println("PARAULA_CLAU_SERVIDOR: \"" + paraulaClauServidor + "\"");

		try {
			// Inicializamos servidor
			System.out.print("> Inicializing server... ");
			serverSocket = new ServerSocket(port);
			System.out.println("OK");
			// Hilo que procesa mensajes FIFO
			Thread processador = new Thread(new Runnable() {
				
				public void run() {
					processarMissatges();
				}
			});

			processador.start();
			
			// Aceptamos clientes mientras el servidor siga activo
			while (!servidorTancant) {
				Socket socket = serverSocket.accept();
				// Si el servidor se está cerrando
				if (servidorTancant) {
					socket.close();
					break;
				}
				// Si superamos el máximo
				if (clientsActius >= maxClients) {
					System.out.println("> Connexió rebutjada: màxim de " + maxClients + " clients assolit.");
					socket.close();
					continue;
				}
				comptadorClients++;
				clientsActius++;

				int numClient = comptadorClients;

				System.out.println("> Connection from client " + numClient + " ... OK");

				ClientHandler handler = new ClientHandler(socket, numClient);
				handlersActius.add(handler);
				haTingutClients = true;

				// Hilo para el cliente
				Thread t = new Thread(handler);
				t.start();
			}

		} catch (IOException e) {
			
			if (!servidorTancant) {
				System.out.println("Error al servidor: " + e.getMessage());
			}
			
		} finally {
			try {
				if (serverSocket != null && !serverSocket.isClosed()) {
					serverSocket.close();
				}
			} catch (IOException e) {
			}
			
			teclat.close();
			System.out.println("> Closing server... OK");
			System.out.println("> Bye!");
		}
	}

	// Procesa mensajes en orden FIFO
	static void processarMissatges() {

		while (!servidorTancant) {
			try {
				String[] entrada = null;
				// Accedemos a la cola FIFO
				synchronized (coadeMissatges) {
					// Si hay mensajes
					if (!coadeMissatges.isEmpty()) {
						// Cogemos el más antiguo
						entrada = coadeMissatges.remove(0);
					}
				}
				// Si no hay mensajes esperamos
				if (entrada == null) {
					Thread.sleep(100);
					continue;
				}

				int numClient = Integer.parseInt(entrada[0]);
				String missatge = entrada[1];
				// Buscamos handler
				ClientHandler handler = buscarHandler(numClient);
				// Si el cliente ya no existe ignoramos
				if (handler == null || !handler.isActiu())
					continue;
				System.out.println("#Rebut del client " + numClient + ": " + missatge);
				// Si cliente usa SU keyword
				if (missatge.equalsIgnoreCase(handler.getParaulaClau())) {
					System.out.println("> Client " + numClient + " keyword detected!");
					handler.tancar();
					continue;
				}

				// Pedimos respuesta al servidor
				System.out.print("#Enviar al client " + numClient + ": ");
				String resposta = teclat.nextLine();
				// Si el servidor usa SU keyword
				if (resposta.equalsIgnoreCase(paraulaClauServidor)) {
					System.out.println("> Server keyword detected! Tancant tots els chats...");
					servidorTancant = true;
					// Cerramos todos los clientes
					ArrayList<ClientHandler> copia = new ArrayList<>(handlersActius);
					
					for (ClientHandler h : copia) {
						h.tancar();
					}
					
					// Cerramos servidor
					try {
						serverSocket.close();
					} catch (IOException e) {
					}
					break;
				}
				// Si el servidor usa keyword de ESTE cliente
				if (resposta.equalsIgnoreCase(handler.getParaulaClau())) {
					System.out.println("> Client " + numClient + " keyword detected!");
					handler.enviarITancar(resposta);
					continue;
				}
				// Respuesta normal
				handler.enviar(resposta);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	// Busca handler de un cliente
	static ClientHandler buscarHandler(int numClient) {
		synchronized (handlersActius) {
			for (ClientHandler h : handlersActius) {
				if (h.getNumClient() == numClient)
					return h;
			}
		}

		return null;
	}

	// Llamado cuando un cliente se desconecta
	static synchronized void clientDesconnectat(int numClient) {
		clientsActius--;
		// Si ya hubo clientes y ahora no queda ninguno
		if (haTingutClients && clientsActius == 0 && !servidorTancant) {
			System.out.println("> No hi ha més clients connectats. Tancant servidor...");
			servidorTancant = true;
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
		}
	}
}