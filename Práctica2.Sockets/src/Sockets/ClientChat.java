package Sockets;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientChat {

	public static void main(String[] args) {

		if (args.length < 2) {
			System.out.println("Ús: java ClientChat <port> <paraula_clau>");
			return;
		}

		int port = Integer.parseInt(args[0]);
		String paraulaClau = args[1];

		Socket socket = null;
		BufferedReader entrada = null; // Llegeix les respostes del servidor
		PrintWriter sortida = null; // Envia missatges al servidor
		Scanner teclat = new Scanner(System.in);

		System.out.println("PORT_SERVIDOR: " + port);
		System.out.println("PARAULA_CLAU_CLIENT: \"" + paraulaClau + "\"");

		try {
			System.out.print("> Inicializing client... ");
			socket = new Socket("localhost", port);
			System.out.println("OK");

			System.out.print("> Inicializing chat... ");
			entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			sortida = new PrintWriter(socket.getOutputStream(), true);
			System.out.println("OK");
			// Enviem la paraula clau com a primer missatge per a guardarla per saber quan tancar la conexio
			sortida.println(paraulaClau);

			boolean continuar = true;

			while (continuar) {

				// eL client sempre parla primer
				System.out.print("#Enviar al servidor: ");
				String missatge = teclat.nextLine();

				sortida.println(missatge);

				// Si el cliente ha utilitzat la seva paraula tanca el seu xat
				if (missatge.equalsIgnoreCase(paraulaClau)) {
					System.out.println("> Client keyword detected!");
					continuar = false;
					break;
				}

				// Esperem a resposta
				// espera fins que servidor respongui
				String resposta = entrada.readLine();

				// null = el servidor ha tancat la conexio
				if (resposta == null) {
					System.out.println("> El servidor ha tancat la connexió.");
					break;
				}

				System.out.println("#Rebut del servidor: " + resposta);

				// Si la resposta del servidor es la nostra paraula clau tanquem el chat
				if (resposta.equalsIgnoreCase(paraulaClau)) {
					System.out.println("> Client keyword detected!");
					continuar = false;
				}
			}

		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			System.out.println("> Closing chat... OK");

			try {
				if (entrada != null)
					entrada.close();
			} catch (IOException e) {
			}

			if (sortida != null)
				sortida.close();

			System.out.print("> Closing client... ");

			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
			}

			teclat.close();

			System.out.println("OK");
			System.out.println("> Bye!");
		}
	}
}