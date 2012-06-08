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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCPClient is the client side network communications class that connects to
 * the CSTA Server.
 * 
 * @author chrismylonas
 */
public class TCPClient implements Runnable {

	/**
	 * 
	 */
	protected Logger clientlog = LoggerFactory.getLogger(TCPClient.class);

	/**
	 * 
	 */
	private char DLE = '\u0010';

	/**
	 * 
	 */
	private byte[] CCCbuffer = new byte[256];

	/**
	 * 
	 */
	private byte[] CCCnewBuf = new byte[256];

	/**
	 * 
	 */
	private boolean CSTAClientCommunications;

	/**
	 * 
	 */
	private StringBuffer CCCrxstr;

	/**
	 * 
	 */
	private boolean CCCcomplete;

	/**
	 * 
	 */
	private boolean CCClastWasDLE;

	/**
	 * 
	 */
	Properties theProps;

	/**
	 * 
	 */
	private String APP_CONFIG_FILE;

	/**
	 * 
	 */
	String APPNAME;

	/**
	 * 
	 */
	int PORT;

	/**
	 * 
	 */
	String SERVER_ADDRESS;

	/**
	 * 
	 */
	private boolean REPLACEDLEWITHDLEDLE = false;

	/**
	 * 
	 */
	private boolean REPLACEDLEDLEWITHDLE = false;

	/**
	 * 
	 */
	private DataOutputStream out;

	/**
	 * 
	 */
	private DataInputStream in;

	/**
	 * 
	 */
	private Socket socket;

	/**
	 * 
	 */
	private byte[] buf;

	/**
	 * 
	 */
	private TCPClientOwnerInterface parent;

	/**
	 * 
	 */
	private boolean runFlag = false;

	/**
	 * 
	 */
	private int line;

	/**
	 * 
	 */
	private StringBuffer chris;

	/**
	 * @param parent
	 * @param _appname
	 * @param _theProps
	 */
	public TCPClient(TCPClientOwnerInterface parent, String _appname,
			Properties _theProps) {
		APPNAME = _appname.toUpperCase();
		theProps = _theProps;
		CSTAClientCommunications = false;
		REPLACEDLEWITHDLEDLE = false;
		REPLACEDLEDLEWITHDLE = false;
		this.parent = parent;
		socket = null;
		Init();
	}

	/**
	 * 
	 */
	public void Init() {
		try {
			clientlog.info(this.getClass().getName() + " -> " + APPNAME
					+ " is the appname");
			clientlog.info(this.getClass().getName() + " -> parent: "
					+ parent.getClass());
			clientlog.info(this.getClass().getName()
					+ " -> Getting property: APP_CONFIG_FILE = "
					+ theProps.getProperty("APP_CONFIG_FILE")
					+ " - locate this file for configuration problems");
			setAPP_CONFIG_FILE(theProps.getProperty("APP_CONFIG_FILE"));
			clientlog.info(this.getClass().getName() + " -> "
					+ "Setting PORT -> Getting property: " + APPNAME
					+ "_SERVER_PORT");
			PORT = Integer.parseInt(theProps.getProperty(APPNAME
					+ "_SERVER_PORT"));
			clientlog.info(this.getClass().getName() + " -> "
					+ "Setting SERVER_ADDRESS -> Getting property: " + APPNAME
					+ "_SERVER_ADDRESS");
			SERVER_ADDRESS = theProps.getProperty(APPNAME + "_SERVER_ADDRESS");
			String logstr = "CSTAServer address: " + SERVER_ADDRESS;
			setSocket(new Socket(SERVER_ADDRESS, PORT));
			logstr += "|||Socket connected to: " + getSocket();
			clientlog.info(this.getClass().getName()
					+ " -> connection details: " + logstr);
			in = new DataInputStream(getSocket().getInputStream());
			out = new DataOutputStream(getSocket().getOutputStream());

		} catch (UnknownHostException e) {
			clientlog.warn(this.getClass().getName() + " -> "
					+ "Unknown host: kq6py.eng");
			// System.exit(1);
		} catch (IOException e) {
			clientlog
					.warn(this.getClass().getName()
							+ " -> "
							+ "No I/O!  The CSTA Server is probably not running or not running for this applications configuration");
			clientlog.error(this.getClass().getName() + " -> " + " check "
					+ getAPP_CONFIG_FILE() + " for " + APPNAME
					+ "_SERVER_PORT and " + APPNAME + "_SERVER_ADDRESS");
			// System.exit(1);
		} catch (NumberFormatException nfe) {
			clientlog.warn(this.getClass().getName() + " -> "
					+ "Number Format Exception: " + PORT);
			nfe.printStackTrace();
			// System.exit(1) ;
		}
		chris = new StringBuffer();
		buf = new byte[1024];
		CCCrxstr = new StringBuffer();
		CCCcomplete = false;
	}

	/**
	 * @return
	 */
	public String GetConnectionInfo() {
		String str = "TCP/IP Connection status:\n\tSocket: " + getSocket();
		return str;
	}

	/**
	 * @param strBuf
	 * @param s_or_r
	 */
	public void WriteToLog(StringBuffer strBuf, char s_or_r) {
		String tmp = Character.toString(s_or_r) + ": ";
		for (int y = 0; y < strBuf.length(); y++)
			tmp += Integer.toHexString(strBuf.charAt(y)) + " ";

		clientlog.info(this.getClass().getName() + " -> CLIENTSERVERCOMMS |"
				+ tmp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		clientlog.info(this.getClass().getName() + " -> "
				+ "run() REPLACEDLEWITHDLEDLE is set to "
				+ REPLACEDLEWITHDLEDLE);
		clientlog.info(this.getClass().getName() + " -> "
				+ "run() REPLACEDLEDLEWITHDLE is set to "
				+ REPLACEDLEDLEWITHDLE);
		clientlog.info(this.getClass().getName() + " -> "
				+ "run() CSTAClientCommunications is set to "
				+ isCSTAClientCommunications());
		if (isCSTAClientCommunications()) {
			internalComms();
		} else {
			this.setRunFlag(true);
			while (runFlag) {
				System.out.println("CHRIS");
				try {
					buf = new byte[1024];
					line = in.read(buf);
					clientlog.info(this.getClass().getName() + " -> " + line
							+ " bytes received");
					buf2SBChris_NC(line);
				} catch (IOException e) {
					clientlog.warn(this.getClass().getName() + " -> "
							+ "Read failed");
				}
			}
		}
		clientlog.warn(this.getClass().getName() + " -> "
				+ " exiting from run()");
		parent.cstaFail();
	}

	/**
	 * 
	 */
	public void Disconnect() {
		try {
			clientlog.info(this.getClass().getName() + " -> "
					+ "INFO - Client Disconnected: Socket closing");
			getSocket().close();
			// **LOG**GENERAL FINE - Network communications disconnection and
			// shut down complete - OK
		} catch (IOException e) {
		}
	}

	/**
	 * @param str
	 */
	public void Send(StringBuffer str) {
		String theString = str.toString();
		WriteToLog(str, 'S');
		if (isREPLACEDLEWITHDLEDLE()) {
			theString = ReplaceDLEwithDLEDLEandWrap(theString);
		}
		try {
			byte[] barray = theString.getBytes("ISO-8859-1");
			out.write(barray);
		} catch (IOException e) {
			clientlog.warn(this.getClass().getName() + " -> "
					+ "WARN IOE tcpclient.Send - " + e.toString());
		} catch (NullPointerException e2) {
			clientlog.warn(this.getClass().getName() + " -> "
					+ "WARN NE tcpclient.Send- " + e2.toString());
		}
	}

	/**
	 * @return
	 */
	public boolean isRunFlag() {
		return runFlag;
	}

	/**
	 * @param runFlag
	 */
	public void setRunFlag(boolean runFlag) {
		this.runFlag = runFlag;
	}

	/**
	 * @param length
	 */
	private void buf2SBChris(int length) {
		for (int i = 0; i < length; i++) {

			if ((short) buf[i] < 0) {
				append2chris((int) buf[i] + 256);
			}

			else {
				Byte b = new Byte(buf[i]);
				append2chris((int) b.intValue());
			}
		}
		checkBuffer();
	}

	/**
	 * @param length
	 */
	private void buf2SBChris_NC(int length) {
		for (int i = 0; i < length; i++) {

			if ((short) buf[i] < 0) {
				append2chris((int) buf[i] + 256);
			}

			else {
				Byte b = new Byte(buf[i]);
				append2chris((int) b.intValue());
			}
		}
		checkBuffer_NC();
	}

	/**
	 * 
	 */
	private void checkBuffer_NC() {
		TestChris(chris);
		if (chris.length() > 1) {
			if (isBufferResetableAndEven_NC(chris)) {
				clientlog.info(this.getClass().getName() + " -> "
						+ "Incoming Buffer is even and being reset");
				parent.addWorkIN(new StringBuffer(chris));
				chris = new StringBuffer();
			} else if (isBufferStillReading_NC(chris)) {
				clientlog.info(this.getClass().getName() + " -> "
						+ "Buffer is still reading");
			} else if (isBufferHoldingMoreThanOneMessage_NC(chris)) {
				clientlog.info(this.getClass().getName() + " -> "
						+ "Buffer is holding more than one message");
				StringBuffer tmp = new StringBuffer(chris.substring(0,
						(((int) chris.charAt(1)) + 2)));
				parent.addWorkIN(new StringBuffer(chris.substring(0,
						(((int) chris.charAt(1)) + 2))));
				chris = new StringBuffer(
						chris.substring(((int) chris.charAt(1) + 2)));
				checkBuffer_NC();
			}
		}
	}

	/**
	 * 
	 */
	private void checkBuffer() {
		if (chris.length() > 1) {
			TestChris(chris);
			if (isBufferResetableAndEven(chris)) {
				clientlog.info(this.getClass().getName() + " -> "
						+ "Incoming Buffer is even and being reset");
				parent.addWorkIN(new StringBuffer(chris));
				chris = new StringBuffer();
			} else if (isBufferStillReading(chris)) {
				clientlog.info(this.getClass().getName() + " -> "
						+ "Buffer is still reading");
			} else if (isBufferHoldingMoreThanOneMessage(chris)) {
				clientlog.info(this.getClass().getName() + " -> "
						+ "Buffer is holding more than one message");
				StringBuffer tmp = new StringBuffer(chris.substring(0,
						(((int) chris.charAt(2)) + 3)));
				parent.addWorkIN(new StringBuffer(chris.substring(0,
						(((int) chris.charAt(2)) + 3))));
				chris = new StringBuffer(
						chris.substring(((int) chris.charAt(2) + 3)));
				checkBuffer();
			}
		}
	}

	/**
	 * @param curOutStr
	 * @return
	 */
	private String ReplaceDLEwithDLEDLEandWrap(String curOutStr) {
		char DLE = '\u0010';
		char ETX = '\u0003';
		StringBuffer curOutStr2 = new StringBuffer();
		char[] tmpDLE = { DLE };
		String DLEasSTR = new String(tmpDLE);
		int length = curOutStr.length();
		for (int i = 0; i < length; i++) {
			if (curOutStr.charAt(i) == DLE) {
				curOutStr2 = curOutStr2.append(DLEasSTR);
			} else
				;

			curOutStr2 = curOutStr2.append(curOutStr.charAt(i));
		}
		char[] dleetx = { DLE, ETX };
		String DLEETX = new String(dleetx);
		curOutStr2 = curOutStr2.append(DLEETX);
		return curOutStr2.toString();
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferResetableAndEven(StringBuffer sb) {
		if (chris.length() == ((int) chris.charAt(2) + 3)) {
			return true;
		}
		return false;
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferHoldingMoreThanOneMessage(StringBuffer sb) {
		if (chris.length() > ((int) chris.charAt(2) + 3)) {
			return true;
		}
		return false;
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferStillReading_NC(StringBuffer sb) {
		String bufLength = Integer.toString(chris.length());
		if (sb.length() > 1) {
			String intendedLength = Integer.toHexString((int) chris.charAt(1));
			clientlog.info(this.getClass().getName() + " -> "
					+ "Buffer status -> Intended Length: " + intendedLength
					+ " | Current Length: " + bufLength);
			if (chris.length() < ((int) chris.charAt(1))) {
				clientlog.info(this.getClass().getName() + " -> "
						+ "Buffer is still reading");
				return true;
			}
			clientlog.info(this.getClass().getName() + " -> "
					+ "Buffer is complete, now ready for clearing");
		} else {
			return false;
		}
		return false;
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferResetableAndEven_NC(StringBuffer sb) {
		if (chris.length() == (int) chris.charAt(1)) {
			return true;
		}
		return false;
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferHoldingMoreThanOneMessage_NC(StringBuffer sb) {
		if (chris.length() > (int) chris.charAt(1)) {
			return true;
		}
		return false;
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferStillReading(StringBuffer sb) {
		String bufLength = Integer.toString(chris.length());
		String intendedLength = Integer.toHexString((int) chris.charAt(2));
		clientlog.info(this.getClass().getName() + " -> "
				+ "Buffer status -> Intended Length: " + intendedLength
				+ " | Current Length: " + bufLength);
		if (chris.length() < (((int) chris.charAt(2)) + 3)) {
			clientlog.info(this.getClass().getName() + " -> "
					+ "Buffer is still reading");
			return true;
		}
		clientlog.info(this.getClass().getName() + " -> "
				+ "Buffer is complete, now ready for clearing");
		return false;
	}

	/**
	 * @param thisByte
	 */
	private void append2chris(int thisByte) {
		chris.append((char) thisByte);
	}

	/**
	 * @param cm
	 */
	public void TestChris(StringBuffer cm) {
		for (int i = 0; i < cm.length(); i++) {
			System.out.print(Integer.toHexString((char) cm.charAt(i)) + " ");
		}
	}

	/**
         * 
         */
	private void internalComms() {
		try {
			while (true) {
				int bytes = in.read(CCCbuffer);
				clientlog.info("####### 1 - read bytes in");
				if (bytes > 0) {
					for (int i = 0; i < bytes; i++) {
						if ((short) CCCbuffer[i] < 0) {
							CCCAddToBuffer((int) CCCbuffer[i] + 256);
						} else {
							CCCAddToBuffer((int) CCCbuffer[i]);
						}
					}
					clientlog.info("####### 2 - bytes read");
					if (CCCcomplete == true) {
						clientlog.info("####### 4");
						if (isREPLACEDLEDLEWITHDLE()) {
							CCCrxstr = ReplaceDLEDLEwithDLEandStrip(CCCrxstr);
						}
						String logStr = "";
						for (int i = 0; i < CCCrxstr.length(); i++) {
							logStr += Integer.toHexString(CCCrxstr.charAt(i))
									+ " ";
						}
						clientlog.info(this.getClass().getName()
								+ " -> SERVERCLIENTCOMMS |" + "R: " + logStr);
						parent.PassedUp(new StringBuffer(CCCrxstr));
					}
					clientlog.info("####### 5");
				}
				if (bytes < 0) {
					// parent.ClientDisconnected(this) ;
					clientlog.warn(this.getClass().getName()
							+ " - in.read returned -1 - Client Disconnected");
					break;
				}
				if (CCCcomplete == true) {
					clientlog.info("####### 6");
					this.CCCbuffer = CCCnewBuf;
					CCCrxstr = new StringBuffer();
					CCCcomplete = false;
				}
			}
			clientlog.info(this.getClass().getName() + " -> "
					+ "Disconnecting......");
			this.setRunFlag(false);
			clientlog.warn(this.getClass().getName() + " -> "
					+ " the run flag has been set to false");
			getSocket().close();
		} catch (IOException e) {
			clientlog
					.warn(this.getClass().getName()
							+ " -> "
							+ " had a bit of trouble with I/O - nothing critical, this will gracefully recover when settings are right");
		} catch (NullPointerException e) {
			clientlog
					.warn(this.getClass().getName()
							+ " -> "
							+ " had a bit of trouble with reading from null! - nothing critical, this will gracefully recover when settings are right");
		} finally {
			try {
				this.setRunFlag(false);
				getSocket().close();
				// **LOG**GENERAL FINE - Network communications for this client
				// shut down complete - OK
			} catch (IOException e) {
				clientlog
						.warn(this.getClass().getName()
								+ " -> "
								+ " socket closing not available - probably never existed - not critical/fatal - just a warning - I/O");
			} catch (NullPointerException e) {
				clientlog
						.warn(this.getClass().getName()
								+ " -> "
								+ " socket closing not available - probably never existed - not critical/fatal - just a warning - Null");
			}
		}
		clientlog.info(this.getClass().getName() + " !! "
				+ " the end of internal communications");

	}

	/**
	 * @param curInStr
	 * @return
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
	 * @return
	 */
	public boolean isCSTAClientCommunications() {
		return CSTAClientCommunications;
	}

	/**
	 * @param CSTAClientCommunications
	 */
	public void setCSTAClientCommunications(boolean CSTAClientCommunications) {
		this.CSTAClientCommunications = CSTAClientCommunications;
	}

	/**
	 * @param currentByte
	 */
	private void CCCAddToBuffer(int currentByte) {
		if (!CCCcomplete) {
			if (!CheckReceived(currentByte))
				CCCrxstr = CCCrxstr.append((char) currentByte);
			else
				CCCcomplete = true;// this is complete, just for the trailing
									// character not to be added.
		} else
			;
	}

	/**
	 * @param currentByte
	 * @return
	 */
	private boolean CheckReceived(int currentByte) {

		if (CCClastWasDLE == true) {
			if (currentByte == 0x10) {
				CCClastWasDLE = false;
				return false;
			} else if (currentByte == 0x03) {
				CCClastWasDLE = false;
				return true;
				/*
				 * BECAUSE WE RETURN TRUE, 0X03 DOESN'T GET ADDED ONTO THE
				 * STRINGBUFFER COS IT KNOWS WE HAVE REACHED THE END.....SO WHEN
				 * IT COMES TIME TO STRIP....DON'T STRIP AN EXTRA CHARACTER OFF
				 * THAT WASNT' PUT ON IN THE FIRST PLACE...
				 */
			} else {
				CCClastWasDLE = false;
				return false;
			}
		} else if (currentByte == 0x10 && CCClastWasDLE == false) {
			// System.out.println("Current byte is 0x10") ;
			CCClastWasDLE = true;
			return false;
		} else {
			return false;
		}

	}

	/**
	 * @return the APP_CONFIG_FILE
	 */
	public String getAPP_CONFIG_FILE() {
		return APP_CONFIG_FILE;
	}

	/**
	 * @param APP_CONFIG_FILE
	 *            the APP_CONFIG_FILE to set
	 */
	public void setAPP_CONFIG_FILE(String APP_CONFIG_FILE) {
		this.APP_CONFIG_FILE = APP_CONFIG_FILE;
	}

	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * @param socket
	 *            the socket to set
	 */
	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	/**
	 * @return the REPLACEDLEWITHDLEDLE
	 */
	public boolean isREPLACEDLEWITHDLEDLE() {
		return REPLACEDLEWITHDLEDLE;
	}

	/**
	 * @param REPLACEDLEWITHDLEDLE
	 *            the REPLACEDLEWITHDLEDLE to set
	 */
	public void setREPLACEDLEWITHDLEDLE(boolean REPLACEDLEWITHDLEDLE) {
		this.REPLACEDLEWITHDLEDLE = REPLACEDLEWITHDLEDLE;
	}

	/**
	 * @return the REPLACEDLEDLEWITHDLE
	 */
	public boolean isREPLACEDLEDLEWITHDLE() {
		return REPLACEDLEDLEWITHDLE;
	}

	/**
	 * @param REPLACEDLEDLEWITHDLE
	 *            the REPLACEDLEDLEWITHDLE to set
	 */
	public void setREPLACEDLEDLEWITHDLE(boolean REPLACEDLEDLEWITHDLE) {
		this.REPLACEDLEDLEWITHDLE = REPLACEDLEDLEWITHDLE;
	}
}
