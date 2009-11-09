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

/**
 * An interface for holding the method declarations of some useful things to do as a server.
 * 
 * 
 * @author mylo
 */
public interface NetworkServer {
    
/**
 * AddTo_listOfClients is used when a client connects and needs to be added to the listOfClients list.  Usually in a monitoring situation.
 * 
 * @param client client connected to the server
 * 
 * @return returns true if successful operation
 * @param x 
 */
    public boolean AddTo_listOfClients(ServeOneClient x) ;
    
/**
 * Writes to log
 * 
 * @param strBuf the hex values as a stringbuffer
 * 
 * @param sb 
 * @param s_or_r s for sent, r for received
 */
    public void WriteToLog(StringBuffer sb, char s_or_r) ;
    
    
/**
 * ClientDisconnected is called when the <U>CSTAServer<U> recognises the client has disconnected.
 * 
 * @param client reference to which client is disconnected
 * 
 * @param x 
 */
    public void ClientDisconnected(ServeOneClient x) ;
    
/**
 * FromClient is called by the client to notify the server that a switching request has been made.
 * 
 * @param str the string received from the client
 * @param client the client which is sending the request
 * 
 * @param sb 
 * @param x 
 */
    public void FromClient(StringBuffer sb, ServeOneClient x) ;
}
