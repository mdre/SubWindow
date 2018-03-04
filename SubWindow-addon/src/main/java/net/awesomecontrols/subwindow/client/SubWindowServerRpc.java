package net.awesomecontrols.subwindow.client;


// ServerRpc is used to pass events from client to server
import com.vaadin.shared.communication.ServerRpc;
import com.vaadin.shared.ui.ClickRpc;

/**
 *
 * @author SShadow
 */
public interface SubWindowServerRpc extends ClickRpc, ServerRpc {

    /**
     *
     * @param newState windowState
     */
    public void windowModeChanged(SubWindowMode newState);

    /**
     *
     * @param x int
     * @param y int
     */
    public void windowMoved(int x, int y);

}