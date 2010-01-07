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

import java.net.* ;
import java.io.* ;
import java.util.Properties ;
import org.apache.log4j.* ;

/**
 * TCPClient is the client side network communications class that connects to the CSTA Server.
 *
 */
public class TCPClient implements Runnable{
	protected Logger clientlog = Logger.getLogger(TCPClient.class) ;
    private char DLE = '\u0010' ;
    private byte[] CCCbuffer = new byte[256] ;
    private byte[] CCCnewBuf = new byte[256] ;
    private boolean CSTAClientCommunications ;
    private StringBuffer CCCrxstr ;
    private boolean CCCcomplete;
    private boolean  CCClastWasDLE ;
	Properties theProps ;
    private String APP_CONFIG_FILE ;
	String APPNAME ;
	int PORT  ;
	String SERVER_ADDRESS ;
	private DataOutputStream out ;
	private DataInputStream in ;
	private Socket socket ;
	private byte[] buf ;
	private TCPClientOwnerInterface parent ;
	private boolean runFlag = false ;
	private int line ;
	private StringBuffer chris ;

//        public static void main(String[] args){
//            TCPClient tcp = new TCPClient("tcpclient") ;
//            tcp.run() ;
//        }
        
//        public TCPClient(String _appname){
//            APPNAME = _appname.toUpperCase() ;
//            theProps = PropertiesController.getInstance(APPNAME) ;
//            Init() ;
//        }
        
	public TCPClient(TCPClientOwnerInterface parent, String _appname,Properties _theProps){
		APPNAME = _appname.toUpperCase() ;
        theProps = _theProps ;
		this.parent = parent ;
        socket = null ;
		Init() ;
	}

	public void Init(){
		try{
			clientlog.info(this.getClass().getName() + " -> " + APPNAME + " is the appname") ;
            clientlog.info(this.getClass().getName() + " -> parent: " + parent.getClass() ) ;
            clientlog.info(this.getClass().getName() + " -> Getting property: APP_CONFIG_FILE = " + theProps.getProperty("APP_CONFIG_FILE") + " - locate this file for configuration problems" ) ;
            setAPP_CONFIG_FILE(theProps.getProperty("APP_CONFIG_FILE"));
            clientlog.info(this.getClass().getName() + " -> " + "Setting PORT -> Getting property: " + APPNAME + "_SERVER_PORT") ;
			PORT = Integer.parseInt( theProps.getProperty(APPNAME + "_SERVER_PORT") ) ;
            clientlog.info(this.getClass().getName() + " -> " + "Setting SERVER_ADDRESS -> Getting property: " + APPNAME + "_SERVER_ADDRESS") ;
			SERVER_ADDRESS = theProps.getProperty(APPNAME + "_SERVER_ADDRESS") ;
			String logstr = "CSTAServer address: " + SERVER_ADDRESS ;
			setSocket(new Socket(SERVER_ADDRESS, PORT)) ;
			logstr += "|||Socket connected to: " + getSocket() ;
			clientlog.info(this.getClass().getName() + " -> connection details: " + logstr) ;
			in = new DataInputStream( getSocket().getInputStream() ) ;
			out = new DataOutputStream( getSocket().getOutputStream() ) ;

		} catch (UnknownHostException e) {
			clientlog.warn(this.getClass().getName() + " -> " + "Unknown host: kq6py.eng");
//			System.exit(1);
		} catch  (IOException e) {
			clientlog.warn(this.getClass().getName() + " -> " + "No I/O!  The CSTA Server is probably not running or not running for this applications configuration") ;
            clientlog.fatal(this.getClass().getName() + " -> " + " check " + getAPP_CONFIG_FILE() + " for " + APPNAME + "_SERVER_PORT and " + APPNAME + "_SERVER_ADDRESS") ;
//            System.exit(1);
		}catch(NumberFormatException nfe){
			clientlog.warn(this.getClass().getName() + " -> " + "Number Format Exception: " + PORT ) ;
			nfe.printStackTrace() ;
//			System.exit(1) ;
		}
		chris = new StringBuffer() ;
		buf = new byte[1024] ;
        CCCrxstr = new StringBuffer() ;
        CCCcomplete = false ;
	}

	public String GetConnectionInfo(){
		String str = "TCP/IP Connection status:\n\tSocket: " + getSocket() ;
		return str ;
	}

	public void WriteToLog(StringBuffer strBuf, char s_or_r){
		String tmp = Character.toString(s_or_r) + ": " ;
		for( int y = 0 ; y < strBuf.length() ; y++ )
			tmp += Integer.toHexString( strBuf.charAt(y) ) + " " ;

		clientlog.info(this.getClass().getName() + " -> CLIENTSERVERCOMMS |" + tmp) ;
	}

	public void run(){
        if( isCSTAClientCommunications() == true ){
            internalComms() ;
        }
        else{
            this.setRunFlag(true) ;
            while(runFlag){
                try{
                    buf = new byte[1024] ;
                    line = in.read(buf);
                    clientlog.info(this.getClass().getName() + " -> " + line + " bytes received") ;
                    buf2SBChris(line) ;
                } catch (IOException e) {
                    clientlog.warn(this.getClass().getName() + " -> " + "Read failed");
                    //System.exit(-1);
                }
            }
        }
        clientlog.warn(this.getClass().getName() + " -> " + " exiting from run()") ;
        parent.cstaFail();
	}

	public void Disconnect(){
		try{
			clientlog.info(this.getClass().getName() + " -> " + "INFO - Client Disconnected: Socket closing") ;
			getSocket().close() ;
			//**LOG**GENERAL FINE - Network communications disconnection and shut down complete - OK
		}catch(IOException e){}
	}


	public void Send(StringBuffer str){
		String theString = str.toString() ;
		WriteToLog(str, 'S') ;
                theString = ReplaceDLEwithDLEDLEandWrap(theString) ;
		try{
            byte[] barray = theString.getBytes() ;
            out.write(barray) ;
//			out.writeBytes(theString) ;
		}catch(IOException e){
			clientlog.warn(this.getClass().getName() + " -> " + "WARN IOE tcpclient.Send - " + e.toString() ) ;
		}catch(NullPointerException e2){
			clientlog.warn(this.getClass().getName() + " -> " + "WARN NE tcpclient.Send- " + e2.toString() ) ;
		}
	}
	public boolean isRunFlag() {
		return runFlag;
	}

	public void setRunFlag(boolean runFlag) {
		this.runFlag = runFlag;
	}
	
	private void buf2SBChris(int length){
		for( int i = 0 ; i < length ; i++){
			
            if( (short)buf[i] < 0 ){
                append2chris( (int)buf[i] + 256 ) ;
            }

            else{
                Byte b = new Byte(buf[i]) ;
                append2chris( (int) b.intValue() ) ;
            }
		}
		checkBuffer() ;
	}
	
	private void checkBuffer(){
		if( chris.length() > 5 ){
			TestChris(chris) ;
			if( isBufferResetableAndEven(chris) ){
				clientlog.info(this.getClass().getName() + " -> " + "Incoming Buffer is even and being reset");
				parent.addWorkIN(new StringBuffer(chris)) ;
				chris = new StringBuffer() ;
			}
			else if( isBufferStillReading(chris) ){
                            clientlog.info(this.getClass().getName() + " -> " + "Buffer is still reading") ;
//				System.out.println("Incoming Buffer is still reading");
//				System.out.println("The length of chris at the moment is: " + Integer.toString(chris.length() ) );
//				System.out.println("The length of chris full message should be (hex): " + Integer.toHexString( (int)chris.charAt(2)) ) ;
//				System.out.println("The length of chris full message should be (dec): " + Integer.toString( chris.charAt(2)) ) ;
			}
			else if( isBufferHoldingMoreThanOneMessage(chris) ){
                            clientlog.info(this.getClass().getName() + " -> " + "Buffer is holding more than one message");
//				System.out.println("Incoming Buffer has over read more than one message") ;
//				System.out.println("The length of chris at the moment is: " + Integer.toString(chris.length() ) );
//				System.out.println("The length of chris full message should be (hex): " + Integer.toHexString( (int)chris.charAt(2)) ) ;
//				System.out.println("The length of chris full message should be (dec): " + Integer.toString( chris.charAt(2)) ) ;
				StringBuffer tmp = new StringBuffer(chris.substring(0, (((int)chris.charAt(2)) + 3 ))) ;
//				System.out.println("Test this tmp string") ;
//				TestChris(tmp) ;
//				System.out.println("Test this tmp string") ;
//				TestChris(tmp) ;
//				System.out.println("Test this tmp string") ;
//				TestChris(tmp) ;
//				System.out.println("Test this tmp string") ;
//				TestChris(tmp) ;
				parent.addWorkIN(new StringBuffer( chris.substring(0, (((int)chris.charAt(2)) + 3)) )) ;
				chris = new StringBuffer(chris.substring( ((int)chris.charAt(2) + 3)))  ;
//				System.out.println("The new active buffered message is") ;
//				TestChris(chris) ;
//				System.out.println("The new active buffered message is") ;
//				TestChris(chris) ;
//				System.out.println("The new active buffered message is") ;
//				TestChris(chris) ;
				checkBuffer() ;
			}
		}
	}
        
    private String ReplaceDLEwithDLEDLEandWrap(String curOutStr){
        char DLE = '\u0010' ;
        char ETX = '\u0003' ;
        //        StringBuffer tmp = new StringBuffer(curOutStr) ;
        StringBuffer curOutStr2 = new StringBuffer();
        char[] tmpDLE= {DLE} ;
        String DLEasSTR = new String( tmpDLE) ;
        //        int tmpDLEcount = 0 ;
        int length = curOutStr.length() ;
        for( int i = 0 ; i < length ; i++){
            if(curOutStr.charAt(i) == DLE){
                //                tmp = tmp.insert(i, DLEasSTR) ;
                curOutStr2 = curOutStr2.append(DLEasSTR) ;
                //                tmpDLEcount++ ;
            } else ;
            
            curOutStr2 = curOutStr2.append( curOutStr.charAt(i) ) ;
        }
        //        curOutStr = tmp.toString() ;
        char[] dleetx = {DLE,ETX} ;
        String DLEETX = new String(dleetx) ;
        //        curOutStr = curOutStr + DLEETX ;
        //		StringContains(curOutStr) ;
        curOutStr2 = curOutStr2.append(DLEETX) ;
        return curOutStr2.toString() ;
    }
	
	private boolean isBufferResetableAndEven(StringBuffer sb){
		if( chris.length() == ((int)chris.charAt(2) + 3) ){
			return true ;
		}
		return false ;
	}
	
	private boolean isBufferHoldingMoreThanOneMessage(StringBuffer sb){
		if( chris.length() > ((int)chris.charAt(2) + 3) ){
			return true ;
		}
		return false ;
	}
	
	private boolean isBufferStillReading(StringBuffer sb){
		String bufLength = Integer.toString(chris.length() ) ;
                String intendedLength = Integer.toHexString((int)chris.charAt(2)) ;
                clientlog.info(this.getClass().getName() + " -> " + "Buffer status -> Intended Length: " + intendedLength + " | Current Length: " + bufLength ) ;
		if( chris.length() < (((int)chris.charAt(2)) + 3) ){
                    clientlog.info(this.getClass().getName() + " -> " + "Buffer is still reading") ;
                    return true ;
		}
                clientlog.info(this.getClass().getName() + " -> " + "Buffer is complete, now ready for clearing") ;
		return false ;
	}
	
	private void append2chris(int thisByte){
		chris.append((char)thisByte) ;
	}
	
	public void TestChris(StringBuffer cm){
//		System.out.print("Client <--- Server | R: ") ;
		for( int i = 0 ; i < cm.length() ; i++ ){
			System.out.print( Integer.toHexString((char)cm.charAt(i)) + " " ) ;
		}
//		System.out.println("") ;
	}
        
        private void internalComms(){
            try{
                while(true){
                    clientlog.info("####### 1") ;
                    int bytes = in.read(CCCbuffer) ;
                    clientlog.info("####### 2") ;
                    if( bytes > 0 ){
                        for( int i = 0 ; i < bytes ; i++ ){
                            if( (short)CCCbuffer[i] < 0 ){
                                CCCAddToBuffer((int)CCCbuffer[i]+256) ;
                            } else{
                                CCCAddToBuffer( (int)CCCbuffer[i] ) ;
                            }
                        }
                        clientlog.info("####### 3") ;
                        if(CCCcomplete == true){
                            clientlog.info("####### 4") ;
                            CCCrxstr = ReplaceDLEDLEwithDLEandStrip(CCCrxstr) ;
                            String logStr = "" ;
                            for(int i = 0 ; i < CCCrxstr.length() ; i++ ){
                                logStr += Integer.toHexString(CCCrxstr.charAt(i)) + " "  ;
                            }
                            clientlog.info(this.getClass().getName() + " -> SERVERCLIENTCOMMS |" + "R: " + logStr ) ;
                            parent.PassedUp(new StringBuffer(CCCrxstr) ) ;
                        }
                        clientlog.info("####### 5") ;
                    }
                    if( bytes < 0 ){
//                        parent.ClientDisconnected(this) ;
                        clientlog.warn(this.getClass().getName() + " - in.read returned -1 - Client Disconnected") ;
                        break ;
                    }
                    if(CCCcomplete == true){
                        clientlog.info("####### 6") ;
                        this.CCCbuffer =  CCCnewBuf ;
                        CCCrxstr = new StringBuffer() ;
                        CCCcomplete = false ;
//                        parent.PassedUp(CCCrxstr);
                    }
                }
                clientlog.info(this.getClass().getName() + " -> " + "Disconnecting......") ;
                this.setRunFlag(false);
                clientlog.warn(this.getClass().getName() + " -> " + " the run flag has been set to false" ) ;
                getSocket().close() ;
            }catch(IOException e){
                clientlog.warn(this.getClass().getName() + " -> " + " had a bit of trouble with I/O - nothing critical, this will gracefully recover when settings are right") ;
            }catch(NullPointerException e){
                clientlog.warn(this.getClass().getName() + " -> " + " had a bit of trouble with reading from null! - nothing critical, this will gracefully recover when settings are right") ;
            }finally{
                try{
                    this.setRunFlag(false);
                    getSocket().close() ;
                    //**LOG**GENERAL FINE - Network communications for this client shut down complete - OK
                }catch(IOException e){
                    clientlog.warn(this.getClass().getName() + " -> " + " socket closing not available - probably never existed - not critical/fatal - just a warning - I/O") ;
                }catch(NullPointerException e){
                    clientlog.warn(this.getClass().getName() + " -> " + " socket closing not available - probably never existed - not critical/fatal - just a warning - Null") ;
                }
            }
            clientlog.info(this.getClass().getName() + " !! " + " the end of internal communications") ;

        }
        
    private StringBuffer ReplaceDLEDLEwithDLEandStrip(StringBuffer curInStr){
        boolean DLEflag = false ;
        int length = curInStr.length() ;
        curInStr = curInStr.deleteCharAt( (length-1) ) ;
        length = length - 1 ;
        StringBuffer curInStr2 = new StringBuffer() ;
        for(int i = 0 ; i < length ; i++){
            if(curInStr.charAt(i) == DLE){
                if(DLEflag == true)
                    DLEflag = false ;
                else{
                    curInStr2 = curInStr2.append(curInStr.charAt(i)) ;
                    DLEflag = true ;
                }
            } else
                curInStr2 = curInStr2.append(curInStr.charAt(i)) ;
        }
        return curInStr2 ;
    }

    public boolean isCSTAClientCommunications() {
        return CSTAClientCommunications;
    }

    public void setCSTAClientCommunications(boolean CSTAClientCommunications) {
        this.CSTAClientCommunications = CSTAClientCommunications;
    }
    
    private void CCCAddToBuffer( int currentByte ){
        if(!CCCcomplete){
            if ( !CheckReceived(currentByte) )
                CCCrxstr = CCCrxstr.append( (char)currentByte ) ;
            else
                CCCcomplete = true ;//this is complete, just for the trailing character not to be added.
        } else ;
    }

    private boolean CheckReceived( int currentByte ){
        
        if( CCClastWasDLE == true ){
            if( currentByte == 0x10 ){
                CCClastWasDLE = false ;
                return false ;
            } else if( currentByte == 0x03 ){
                CCClastWasDLE = false ;
                return true ;
                                /* BECAUSE WE RETURN TRUE, 0X03 DOESN'T GET ADDED ONTO THE STRINGBUFFER COS IT KNOWS
                                WE HAVE REACHED THE END.....SO WHEN IT COMES TIME TO STRIP....DON'T STRIP AN EXTRA
                                CHARACTER OFF THAT WASNT' PUT ON IN THE FIRST PLACE...*/
            } else{
                CCClastWasDLE = false ;
                return false ;
            }
        } else if( currentByte == 0x10 && CCClastWasDLE==false){
            //System.out.println("Current byte is 0x10") ;
            CCClastWasDLE = true ;
            return false ;
        } else{
            return false ;
        }
        
    }

    /**
     * @return the APP_CONFIG_FILE
     */
    public String getAPP_CONFIG_FILE() {
        return APP_CONFIG_FILE;
    }

    /**
     * @param APP_CONFIG_FILE the APP_CONFIG_FILE to set
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
     * @param socket the socket to set
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
