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

package org.opencsta.communications;


/*
Open Source CSTA (oscsta) creates a link between the computing network
and the switching network.  Other libraries are used to configure and communicate
with the network.  These libraries and the CSTA-Client are LGPL licensed.
Copyright (C) 2006  mrvoip.com.au - Christopher Mylonas
*/

/**
 * Any communications stream, network or serial, implements this method.
 *
 *
 * @author mylo
 */
public interface CommunicationsStream {
    
    /**
     * The String that will be sent goes through this method...test
     *
     *
     * @return true when successful
     * @param sb The string to be sent
     */
    public boolean SendString(StringBuffer sb) ;
    
    /**
     * Checks the string received to make sure it is valid and complete.
     *
     *
     * @return true if successfully received a full string
     * @param sb the string to be checked
     */
    public boolean CheckReceived(StringBuffer sb) ;
    public void closeComms();
    public void openComms();
}
