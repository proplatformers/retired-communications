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

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.apache.log4j.*;

/**
 * This class, when instantiated, is used to handle one and only one client
 * connection to the CSTAServer. The CSTAServer can handle multiple client
 * conntions by maintaining a List of ServeOneClient objects. The opposite of
 * this class on the client side is TCPClient.
 * 
 * @author chrismylonas
 */
public class ServeOneClient extends Thread {

	/**
	 * 
	 */
	protected static Logger alog = Logger.getLogger(MVListeningThread.class);

	/**
	 * DLE character 0x10
	 * 
	 */
	private char DLE = '\u0010';

	/**
	 * ETX character 0x03
	 * 
	 */
	private char ETX = '\u0003';

	/**
	 * string representation of 0x10
	 */
	private String DLEasSTR;

	/**
	 * string representation of 0x10 0x03
	 */
	private String DLEETX;

	/**
	 * the socket that carries this ServeOneClient's tcp/ip connection
	 * 
	 */
	private Socket socket;

	/**
	 * The CSTAServer. It is the parent of this object and manages it
	 * 
	 */
	private NetworkServer parent;

	/**
	 * the buffer that gets written to when a client sends data to the server
	 * 
	 */
	private StringBuffer rxstr;

	/**
	 * the flag that indicates that a transmission from the client is complete
	 * 
	 */
	private boolean complete;

	/**
	 * indicates that the last received char in the transmission was a DLE char
	 * 
	 */
	private boolean lastWasDLE;

	/**
	 * the socket's input stream
	 * 
	 */
	private DataInputStream in;// from BufferedReader

	/**
	 * the socket that carries this ServeOneClient's tcp/ip connection
	 * 
	 */
	private DataOutputStream out;

	/**
	 * the socket's output stream
	 * 
	 */
	private byte[] newBuf = new byte[256];

	/**
	 * a buffer
	 * 
	 */
	private byte[] buffer = new byte[256];

	/**
     * 
     */
	private boolean running;

	/**
	 * Constructor for this object
	 * 
	 * 
	 * @param s
	 *            the socket in this tcp/ip connection
	 * @param server
	 *            the CSTAServer that handles this ServeOneClient object
	 * @throws IOException
	 *             the exception thrown if the input/output streams aren't set
	 *             up properly
	 */
	public ServeOneClient(Socket s, NetworkServer server) throws IOException {
		this.socket = s;
		this.parent = server;

		if (this.parent.AddTo_listOfClients(this))
			alog.info("ServeOneClient<init> - Server added this client to list");
		else
			alog.warn("ServeOneClient<init> - Server DID NOT add this client to list");

		Init();
		start();
	}

	/**
	 * does some initialisation stuff
	 * 
	 */
	private void Init() throws IOException {
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		rxstr = new StringBuffer();
		complete = false;
		lastWasDLE = false;
		char[] tmpDLE = { DLE };
		DLEasSTR = new String(tmpDLE);
		char[] dleetx = { DLE, ETX };
		DLEETX = new String(dleetx);
	}

	/**
	 * encodes a string to send out back to the client by replacing all DLE
	 * characters with DLE DLE sets and wraps it in the communications protocol
	 * 
	 * 
	 * @return the encoded string
	 * @param curOutStr
	 *            the string to encode
	 */
	private String ReplaceDLEwithDLEDLEandWrap(String curOutStr) {
		StringBuffer tmp = new StringBuffer(curOutStr);
		int tmpDLEcount = 0;
		int length = tmp.length();
		for (int i = 0; i < length; i++) {
			if (tmp.charAt(i + tmpDLEcount) == DLE) {
				tmp = tmp.insert(i, DLEasSTR);
				tmpDLEcount++;
			} else
				;
		}
		curOutStr = tmp.toString();
		curOutStr = curOutStr + DLEETX;
		return curOutStr;
	}

	/**
	 * This method is called by the CSTAServer and sends the string back to the
	 * client which is on the other end of this tcp/ip connection.
	 * 
	 * 
	 * @param str
	 *            the string to send
	 */
	public void SendToClient(StringBuffer str) {
		String theString = str.toString();
		theString = ReplaceDLEwithDLEDLEandWrap(theString);
		String tmp = new String();
		for (int i = 0; i < str.length(); i++)
			tmp += Integer.toHexString(str.charAt(i)) + " ";
		parent.WriteToLog(str, 'S');
		alog.info(this.getClass().getName() + " ---> "
				+ "SERVER TO CLIENT - R: " + tmp);
		try {
			out.writeBytes(theString);
		} catch (IOException e) {
			System.err.println(e);
			parent.ClientDisconnected(this);
		}
	}

	/**
	 * decodes a string to send out back to the client by replacing all DLE DLE
	 * sets with DLE characters and strips off what is left from the
	 * communications protocol
	 * 
	 * 
	 * @param currentByte
	 *            the current byte in the transmission to be added to the buffer
	 */
	private void AddToBuffer(int currentByte) {
		if (!complete) {
			if (!CheckReceived(currentByte))
				rxstr = rxstr.append((char) currentByte);
			else
				complete = true;// this is complete, just for the trailing
								// character not to be added.
		} else
			;
	}

	/**
	 * checks the current received character and acts on it accordingly
	 * 
	 * 
	 * @return returns true when transmission is complete
	 * @param currentByte
	 *            the current byte to be checked
	 */
	private boolean CheckReceived(int currentByte) {
		if (lastWasDLE == true) {
			if (currentByte == 0x10) {
				lastWasDLE = false;
				return false;
			} else if (currentByte == 0x03) {
				// System.out.println("Checkreceived returning true...we have got a full string")
				// ;
				lastWasDLE = false;
				return true;
				/*
				 * BECAUSE WE RETURN TRUE, 0X03 DOESN'T GET ADDED ONTO THE
				 * STRINGBUFFER COS IT KNOWS WE HAVE REACHED THE END.....SO WHEN
				 * IT COMES TIME TO STRIP....DON'T STRIP AN EXTRA CHARACTER OFF
				 * THAT WASNT' PUT ON IN THE FIRST PLACE...
				 */
			} else {
				lastWasDLE = false;
				return false;
			}
		} else if (currentByte == 0x10 && lastWasDLE == false) {
			// System.out.println("Current byte is 0x10") ;
			lastWasDLE = true;
			return false;
		} else {
			return false;
		}
	}

	/**
	 * decodes a string to send out back to the client by replacing all DLE DLE
	 * sets with DLE characters and strips off what is left from the
	 * communications protocol
	 * 
	 * 
	 * @return the decoded string
	 * @param curInStr
	 *            the string to decode
	 */
	private StringBuffer ReplaceDLEDLEwithDLEandStrip(StringBuffer curInStr) {
		boolean DLEflag = false;
		int length = curInStr.length();
		curInStr = curInStr.deleteCharAt((length - 1));
		length = length - 1;
		StringBuffer curInStr2 = new StringBuffer();
		for (int i = 0; i < length; i++) {
			if (curInStr.charAt(i) == DLE) {
				if (DLEflag == true)
					DLEflag = false;
				else {
					curInStr2 = curInStr2.append(curInStr.charAt(i));
					DLEflag = true;
				}
			} else
				curInStr2 = curInStr2.append(curInStr.charAt(i));
		}
		return curInStr2;
	}

	/**
	 * receives data from the tcp/ip socket and adds it to the buffer until
	 * reception is complete. The data is then decoded and sent to the
	 * CSTAServer for processing
	 * 
	 */
	public void run() {
		this.setRunning(true);
		try {
			while (this.isRunning()) {
				int bytes = in.read(buffer);
				if (bytes > 0) {
					for (int i = 0; i < bytes; i++) {
						if ((short) buffer[i] < 0) {
							AddToBuffer((int) buffer[i] + 256);
						} else {
							AddToBuffer((int) buffer[i]);
						}
					}
					if (complete == true) {
						rxstr = ReplaceDLEDLEwithDLEandStrip(rxstr);
						// **LOG**RXTX FINE = Network communications - completed
						// message received <string> - OK
						parent.FromClient(rxstr, this);
					}
				}
				if (bytes < 0) {
					parent.ClientDisconnected(this);
					alog.warn("ServeOneClient.run() - in.read returned -1 - Client Disconnected");
					break;
				}
				if (complete == true) {
					this.buffer = newBuf;
					rxstr = new StringBuffer();
					complete = false;
				}
			}
			alog.info("ServeOneClient.run() - Client Disconnected - Server closing this finished connection");
		} catch (IOException e) {
		} finally {
			try {
				socket.close();
				// **LOG**GENERAL FINE - Network communications for this client
				// shut down complete - OK
			} catch (IOException e) {
			}
		}
	}

	public void Disconnect() {
		this.setRunning(false);
	}

	/**
	 * @return the running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * @param running
	 *            the running to set
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}
}