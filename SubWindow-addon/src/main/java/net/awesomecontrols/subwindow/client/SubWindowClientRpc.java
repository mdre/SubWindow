package net.awesomecontrols.subwindow.client;

import com.vaadin.shared.communication.ClientRpc;

// ClientRpc is used to pass events from server to client
// For sending information about the changes to component state, use State instead

/**
 *
 * @author SShadow
 */
public interface SubWindowClientRpc extends ClientRpc {

    // Example API: Fire up alert box in client

    /**
     *
     * @param message message
     */
    public void alert(String message);

}