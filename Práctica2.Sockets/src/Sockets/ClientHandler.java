package Sockets;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

	private Socket socket;
	private int numClient;
	private BufferedReader entrada; // lee mensajes del cliente
	private PrintWriter sortida; // envía mensajes al cliente
	private String paraulaClau; // palabra clave de ESTE cliente
	private volatile boolean actiu = true;

	public ClientHandler(Socket socket, int numClient) {
		this.socket = socket;
		this.numClient = numClient;
	}

	@Override
	public void run() {
		try {
			entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			sortida = new PrintWriter(socket.getOutputStream(), true);

			// El primer mstg que envia el client es la seva paraula clau, el servidor la guarda per a poder detectarla
			paraulaClau = entrada.readLine();

			System.out.println("> Inicializing chat... OK");
			// Llegim els missatges del client i els fiquem a la cua
			while (actiu && !ServidorChat.servidorTancant) {
				String missatge = entrada.readLine();
				// null = el client ha tancat la conexio
				if (missatge == null)
					break;

				// Afegim a la cua el missatge
				synchronized (ServidorChat.coadeMissatges) {
					ServidorChat.coadeMissatges.add(
							new String[] {
									String.valueOf(numClient),
									missatge
							});
				}
			}

		} catch (IOException e) {
			if (actiu && !ServidorChat.servidorTancant) {
				System.out.println("Error amb client " + numClient + ": " + e.getMessage());
			}
		} finally {
			tancar();
		}
	}

	// Envía un missatge al client
	public synchronized void enviar(String missatge) {
		if (actiu && sortida != null) {
			sortida.println(missatge);
		}
	}

	// Envia un misasatge i despres tanca el xat
	public synchronized void enviarITancar(String missatge) {
		enviar(missatge);
		tancarRecursos();
	}

	// Tanca el xat sense evitar cap missatge
	public synchronized void tancar() {
		tancarRecursos();
	}

	// Tanca tots els reucros d aquest client
	private void tancarRecursos() {
		if (!actiu)
			return; // si ja esta tancat evitam doble tancament
		actiu = false;
		try {
			if (entrada != null)
				entrada.close();
		} catch (IOException e) {
		}
		if (sortida != null)
			sortida.close();
		try {
			if (socket != null && !socket.isClosed())
				socket.close();
		} catch (IOException e) {
		}
		ServidorChat.handlersActius.remove(this);
		ServidorChat.clientDesconnectat(numClient);
	}

	public int getNumClient() {
		return numClient;
	}

	public String getParaulaClau() {
		return paraulaClau;
	}

	public boolean isActiu() {
		return actiu;
	}
}