/*
 * ============================================================================
 *                   The Apache Software License, Version 1.1
 * ============================================================================
 *
 *    Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must  retain the above copyright  notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include  the following  acknowledgment:  "This product includes  software
 *    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
 *    Alternately, this  acknowledgment may  appear in the software itself,  if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "log4j" and  "Apache Software Foundation"  must not be used to
 *    endorse  or promote  products derived  from this  software without  prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products  derived from this software may not  be called "Apache", nor may
 *    "Apache" appear  in their name,  without prior written permission  of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 * APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 * ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 * (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software  consists of voluntary contributions made  by many individuals
 * on  behalf of the Apache Software  Foundation.  For more  information on the
 * Apache Software Foundation, please see <http://www.apache.org/>.
 *
 */

package org.apache.log4j.net;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.plugins.Pauseable;
import org.apache.log4j.plugins.Plugin;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;


/**
  XMLSocketReceiver receives a remote logging event via XML on a configured
  socket and "posts" it to a LoggerRepository as if the event were
  generated locally. This class is designed to receive events from
  the XMLSocketAppender class (or classes that send compatible events).

  <p>Once the event has been "posted", it will be handled by the
  appenders currently configured in the LoggerRespository.

  @author Mark Womack
  @author Scott Deboy <sdeboy@apache.org>

    @since 1.3
*/
public class XMLSocketReceiver extends Receiver implements Runnable, PortBased, Pauseable {
  protected int port;
  protected boolean active = false;
  private boolean paused;
  //default to log4j xml decoder
  protected String decoder = "org.apache.log4j.xml.XMLDecoder";
  private ServerSocket serverSocket;
  private List socketList = new Vector();
  private Thread rThread;
  public static int DEFAULT_PORT = 4448;

  public XMLSocketReceiver() {
  }

  public XMLSocketReceiver(int _port) {
    port = _port;
  }

  public XMLSocketReceiver(int _port, LoggerRepository _repository) {
    port = _port;
    repository = _repository;
  }

  /**
    Get the port to receive logging events on. */
  public int getPort() {
    return port;
  }

  /**
    Set the port to receive logging events on. */
  public void setPort(int _port) {
    port = _port;
  }

  public String getDecoder() {
    return decoder;
  }

  public void setDecoder(String _decoder) {
    decoder = _decoder;
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean b) {
    paused = b;
  }

  /**
   * Returns true if the receiver is the same class and they are
   * configured for the same properties, and super class also considers
   * them to be equivalent. This is used by PluginRegistry when determining
   * if the a similarly configured receiver is being started.
   * 
   * @param testPlugin The plugin to test equivalency against.
   * @return boolean True if the testPlugin is equivalent to this plugin.
   */
  public boolean isEquivalent(Plugin testPlugin) {
    if ((testPlugin != null) && testPlugin instanceof XMLSocketReceiver) {
      XMLSocketReceiver sReceiver = (XMLSocketReceiver) testPlugin;

      return (port == sReceiver.getPort() && super.isEquivalent(testPlugin));
    }

    return false;
  }

  public int hashCode() {
  	
  	int result = 37 * (repository != null? repository.hashCode():0);
  	result = result * 37 + port;
  	return (result * 37 + (getName() != null? getName().hashCode():0));
  }
  	
  /**
    Returns true if this receiver is active. */
  public synchronized boolean isActive() {
    return active;
  }

  /**
    Starts the SocketReceiver with the current options. */
  public void activateOptions() {
    if (!isActive()) {
      rThread = new Thread(this);
      rThread.setDaemon(true);
      rThread.start();
      active = true;
    }
  }

  /**
    Called when the receiver should be stopped. Closes the
    server socket and all of the open sockets. */
  public synchronized void shutdown() {
    // mark this as no longer running
    active = false;

    if (rThread != null) {
      rThread.interrupt();
      rThread = null;
    }
    doShutdown();
  }

    /**
     * Does the actual shutting down by closing the server socket
     * and any connected sockets that have been created.
     */
    private synchronized void doShutdown() {
      active = false;

      LogLog.debug(getName() + " doShutdown called");

      // close the server socket
      closeServerSocket();

      // close all of the accepted sockets
      closeAllAcceptedSockets();
    }

    /**
      * Closes the server socket, if created.
      */
     private void closeServerSocket() {
       LogLog.debug(getName() + " closing server socket");

       try {
         if (serverSocket != null) {
           serverSocket.close();
         }
       } catch (Exception e) {
         // ignore for now
       }

       serverSocket = null;
     }

    /**
      * Closes all the connected sockets in the List.
      */
     private synchronized void closeAllAcceptedSockets() {
       for (int x = 0; x < socketList.size(); x++) {
         try {
           ((Socket) socketList.get(x)).close();
         } catch (Exception e) {
           // ignore for now
         }
       }

       // clear member variables
       socketList.clear();
     }

  /**
    Loop, accepting new socket connections. */
  public void run() {
      /**
        * Ensure we start fresh.
        */
    LogLog.debug("performing socket cleanup prior to entering loop for " + name);
    closeServerSocket();
    closeAllAcceptedSockets();
    LogLog.debug("socket cleanup complete for " + name);       
    active = true;

    // start the server socket
    try {
      serverSocket = new ServerSocket(port);
    } catch (Exception e) {
      LogLog.error(
        "error starting SocketReceiver (" + this.getName()
        + "), receiver did not start", e);
      active = false;
      doShutdown();

      return;
    }

    Socket socket = null;

    try {
      LogLog.debug("in run-about to enter while isactiveloop");

      active = true;

      while (!rThread.isInterrupted()) {
        // if we have a socket, start watching it
        if (socket != null) {
          LogLog.debug("socket not null - creating and starting socketnode");
          socketList.add(socket);

          XMLSocketNode node = new XMLSocketNode(decoder, socket, this);
          new Thread(node).start();
          socket = null;
        }

        LogLog.debug("waiting to accept socket");

        // wait for a socket to open, then loop to start it
        socket = serverSocket.accept();
        LogLog.debug("accepted socket");
      }

      // socket not watched because we a no longer running
      // so close it now.
      if (socket != null) {
        socket.close();
      }
    } catch (Exception e) {
      LogLog.warn(
        "socket server disconnected, stopping");
    }
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.plugins.Receiver#doPost(org.apache.log4j.spi.LoggingEvent)
   */
  public void doPost(LoggingEvent event) {
    if(!isPaused()){
      super.doPost(event);
    }
  }


}
