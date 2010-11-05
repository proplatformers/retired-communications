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

/**
 * This interface is implemented by the parent of a TCPClient. It only has one
 * method to contain, being PassedUp.
 * 
 * 
 * @author chrismylonas
 */
public interface TCPClientOwnerInterface {

	/**
	 * The only required method in this interface. This is the method that is
	 * called by TCPClient once a string has been received and needs processing.
	 * An implementor of this interface is more often than not the
	 * CSTAClientBase class.
	 * 
	 * @return
	 * @param curInStr
	 *            StringBuffer that has been received and needs to be processed
	 */
	boolean PassedUp(StringBuffer curInStr);

	/**
	 * @return
	 */
	public Socket getSocket();

	/**
	 * @param str
	 */
	void addWorkIN(StringBuffer str);

	/**
     * 
     */
	public void cstaFail();
}
