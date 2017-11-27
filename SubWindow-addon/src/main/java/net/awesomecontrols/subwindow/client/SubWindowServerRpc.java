package net.awesomecontrols.subwindow.client;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.communication.ServerRpc;

// ServerRpc is used to pass events from client to server
import com.vaadin.shared.communication.ServerRpc;
import com.vaadin.shared.ui.ClickRpc;
import com.vaadin.shared.ui.window.WindowMode;

public interface SubWindowServerRpc extends ClickRpc, ServerRpc {

    public void windowModeChanged(WindowMode newState);

    public void windowMoved(int x, int y);

}