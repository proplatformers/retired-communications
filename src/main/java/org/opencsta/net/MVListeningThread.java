/*
This file is part of Open CSTA.

    Open CSTA is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open CSTA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Open CSTA.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opencsta.net;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import org.apache.log4j.*;

/**
 * This class is used for listening for a client connection by the server. When
 * a connection is accepted a ServeOneClient object is instantiated.
 * 
 * @author chrismylonas
 */
public class MVListeningThread implements Runnable {

	/**
	 * 
	 */
	protected static Logger alog = Logger.getLogger(MVListeningThread.class);

	/**
	 * The owner of this listening thread
	 * 
	 */
	private NetworkServer parent;

	/**
	 * The ServerSocket that listens for clients to make a connection.
	 * 
	 */
	private ServerSocket s;

	/**
	 * The port to listen on.
	 * 
	 */
	private int PORT;

	/**
	 * 
	 */
	private boolean listening;

	/**
	 * Constructor for this object
	 * 
	 * 
	 * @param server
	 *            The server that owns this listening thread
	 * @param _port
	 *            The port to listen to
	 */
	public MVListeningThread(NetworkServer server, int _port) {
		alog.info(this.getClass().getName() + " about to start on port "
				+ Integer.toString(_port));
		this.parent = server;
		this.PORT = _port;
		Init();
	}

	/**
	 * Constructor when the port is not passed in the constructor.
	 * 
	 * 
	 * @param server
	 */
	public MVListeningThread(NetworkServer server) {
		alog.info(this.getClass().getName()
				+ " about to start on default port 8996");
		this.parent = server;
		PORT = 8996;
		Init();
	}

	/**
	 * Init creates a new server socket and logs the network address and port of
	 * this listening server.
	 * 
	 */
	public void Init() {
		try {
			s = new ServerSocket(PORT);// all addresses 0.0.0.0/0.0.0.0
			// s = new ServerSocket(PORT,5,InetAddress.getLocalHost() )
			// ;//csta.beach.mrvoip.com.au/127.0.0.1
			// byte[] byteaddr = {(byte)192,(byte)168,(byte)0,(byte)40} ;
			// s = new ServerSocket(PORT,5,InetAddress.getByAddress(byteaddr))
			// ;//using byte[] byteaddr
			s.setSoTimeout(6000);
			// THIS WILL WORK FOR IP ADDRESS s = new ServerSocket(PORT,5,
			// InetAddress.getLocalHost()) ;
			alog.info("******** Server address: " + s.getInetAddress() + ":"
					+ PORT);
			this.setListening(true);
		} catch (IOException e) {
			e.printStackTrace();
			alog.warn("******** Server Socket not started - Please check TCP/IP settings");
		}
	}

	/**
	 * Waits for a connection.
	 * 
	 */
	public void run() {
		try {
			Listen();
		} catch (IOException e) {
		} catch (NullPointerException e) {
		}
	}

	/**
	 * Loops after accepting an incoming socket connection.
	 * 
	 * 
	 * @throws IOException
	 *             some io exception
	 * @throws NullPointerException
	 *             some null pointer exception
	 */
	private void Listen() throws IOException, NullPointerException {
		Socket socket = null;
		while (this.isListening()) {
			try {
				alog.info("******** Listening mode: Waiting for a client connection...");
				socket = s.accept();
				Socket newSock = socket;
				new ServeOneClient(newSock, parent);
			} catch (SocketTimeoutException e2) {
				alog.warn("Server socket timeout");
			} catch (IOException e) {
				socket.close();
			}
		}
		s.close();
		alog.info("Listening Thread is halting");
	}

	/**
	 * @return the listening
	 */
	public boolean isListening() {
		return listening;
	}

	/**
	 * @param listening
	 *            the listening to set
	 */
	public void setListening(boolean listening) {
		this.listening = listening;
	}
}