package net.awesomecontrols.subwindow;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.BlurNotifier;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.FocusNotifier;
import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.Connector;
import com.vaadin.shared.EventId;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.window.WindowRole;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.declarative.DesignAttributeHandler;
import com.vaadin.ui.declarative.DesignContext;
import com.vaadin.ui.declarative.DesignException;
import com.vaadin.util.ReflectTools;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.awesomecontrols.subwindow.client.SubWindowMode;
import net.awesomecontrols.subwindow.client.SubWindowServerRpc;
import net.awesomecontrols.subwindow.client.SubWindowState;

/**
 * A component that represents a floating popup window that can be added to a {@link WindowsDesktopArea}. A subwindow is added to a
 * {@code WindowsDesktopArea} using {@link WindowsDesktopArea#addSubWindow(SubWindow)}.
 * <p>
 * The subwindow component is build on the window component and have all it behaviors. The contents of a subwindow is set using
 * {@link #setContent(Component)} or by using the {@link #SubWindow(String, Component)} constructor.
 * </p>
 * <p>
 * A subwindow can be positioned on the screen using relative coordinates (pixels) or set to be centered using {@link #center()}
 * </p>
 * <p>
 * The caption is displayed in the window header.
 * </p>
 * <p>
 *
 * @author Marcelo D. RE.
 */
@SuppressWarnings({"serial", "deprecation"})
public class SubWindow extends Panel
        implements FocusNotifier, BlurNotifier {

    private final static Logger LOGGER = Logger.getLogger(SubWindow.class.getName());

    static {
        LOGGER.setLevel(Level.INFO);
    }

    private SubWindowServerRpc rpc = new SubWindowServerRpc() {

        @Override
        public void click(MouseEventDetails mouseDetails) {
            fireEvent(new ClickEvent(SubWindow.this, mouseDetails));
        }

        @Override
        public void windowModeChanged(SubWindowMode newState) {
            setWindowMode(newState);
        }

        @Override
        public void windowMoved(int x, int y) {
            if (x != getState(false).positionX) {
                setPositionX(x);
            }
            if (y != getState(false).positionY) {
                setPositionY(y);
            }
        }
    };

    /**
     * Holds registered CloseShortcut instances for query and later removal
     */
    private List<CloseShortcut> closeShortcuts = new ArrayList<>(4);

    /**
     * Used to keep the subwindow order position. Order position for unattached subwindow is {@code -1}.
     * <p>
     * Window with greatest order position value is on the top and window with 0 position value is on the bottom.
     */
    private int orderPosition = -1;

    /**
     * Creates a new, empty subwindow.
     */
    public SubWindow() {
        this("", null);
    }

    /**
     * Creates a new, empty subwindow with a given title.
     *
     * @param caption the title of the subwindow.
     */
    public SubWindow(String caption) {
        this(caption, null);
    }

    /**
     * Creates a new, empty subwindow with the given content and title.
     *
     * @param caption the title of the subwindow.
     * @param content the contents of the subwindow
     */
    public SubWindow(String caption, Component content) {
        super(caption, content);
        registerRpc(rpc);
        setSizeUndefined();
        setCloseShortcut(KeyCode.ESCAPE);
    }

    /* ********************************************************************* */

 /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Panel#paintContent(com.vaadin.server.PaintTarget)
     */
    @Override
    public synchronized void paintContent(PaintTarget target)
            throws PaintException {
        if (bringToFront != null) {
            target.addAttribute("bringToFront", bringToFront.intValue());
            bringToFront = null;
        }

        // Contents of the subwindow panel is painted
        super.paintContent(target);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.AbstractComponent#setParent(com.vaadin.server.
     * ClientConnector )
     */
    @Override
    public void setParent(HasComponents parent) {
        if (parent == null
                //                || parent instanceof UI 
                || (parent instanceof WindowsDesktopLayout)) {
            super.setParent(parent);
            // set the Window behavior

        } else {
            throw new IllegalArgumentException(
                    "A SubWindow can only be added to a SubWindowDesktop using .addWindow(SubWindow window)");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Panel#changeVariables(java.lang.Object, java.util.Map)
     */
    @Override
    public void changeVariables(Object source, Map<String, Object> variables) {

        // TODO Are these for top level subwindows or sub subwindows?
        boolean sizeHasChanged = false;
        // size is handled in super class, but resize events only in subwindows ->
        // so detect if size change occurs before super.changeVariables()
        if (variables.containsKey("height") && (getHeightUnits() != Sizeable.Unit.PIXELS
                || (Integer) variables.get("height") != getHeight())) {
            sizeHasChanged = true;
        }
        if (variables.containsKey("width") && (getWidthUnits() != Sizeable.Unit.PIXELS
                || (Integer) variables.get("width") != getWidth())) {
            sizeHasChanged = true;
        }

        super.changeVariables(source, variables);

        // Positioning
        final Integer positionx = (Integer) variables.get("positionx");
        if (positionx != null) {
            final int x = positionx.intValue();
            // This is information from the client so it is already using the
            // position. No need to repaint.
            setPositionX(x < 0 ? -1 : x);
        }
        final Integer positiony = (Integer) variables.get("positiony");
        if (positiony != null) {
            final int y = positiony.intValue();
            // This is information from the client so it is already using the
            // position. No need to repaint.
            setPositionY(y < 0 ? -1 : y);
        }

        if (isClosable()) {
            // Closing
            final Boolean close = (Boolean) variables.get("close");
            if (close != null && close.booleanValue()) {
                close();
            }
        }

        // fire event if size has really changed
        if (sizeHasChanged) {
            fireResize();
        }

        if (variables.containsKey(FocusEvent.EVENT_ID)) {
            fireEvent(new FocusEvent(this));
        } else if (variables.containsKey(BlurEvent.EVENT_ID)) {
            fireEvent(new BlurEvent(this));
        }

    }

    /**
     * Method that handles subwindow closing (from UI).
     *
     * <p>
     * By default, subwindows are removed from their respective UIs and thus visually closed on browser-side.
     * </p>
     *
     * <p>
     * To react to a subwindow being closed (after it is closed), register a {@link CloseListener}.
     * </p>
     */
    public void close() {

        // Two parent sisnce WindowDesktop extends Composite and has a Panel that have a CssLayout inside
        ((SubWindowDesktop) getParent()  // SubWindowsDesktop
                            .getParent() // Panel
                            .getParent() // VerticalLayout
                            .getParent() // WindowsDesktopArea
                            .getParent() // WindowsDesktopLayout
                ).removeWindow(this);

    }

    /**
     * Gets the distance of Window left border in pixels from left border of the containing (main subwindow) when the subwindow is in
     * {@link SubWindowMode#NORMAL}.
     *
     * @return the Distance of Window left border in pixels from left border of the containing (main subwindow).or -1 if unspecified
     * @since 4.0.0
     */
    public int getPositionX() {
        return getState(false).positionX;
    }

    /**
     * Sets the position of the subwindow on the screen using {@link #setPositionX(int)} and {@link #setPositionY(int)}.
     *
     * @since 7.5
     * @param x The new x coordinate for the subwindow
     * @param y The new y coordinate for the subwindow
     */
    public void setPosition(int x, int y) {
        setPositionX(x);
        setPositionY(y);
    }

    /**
     * Sets the distance of Window left border in pixels from left border of the containing (main subwindow). Has effect only if in
     * {@link SubWindowMode#NORMAL} mode.
     *
     * @param positionX the Distance of Window left border in pixels from left border of the containing (main subwindow). or -1 if unspecified.
     * @since 4.0.0
     */
    public void setPositionX(int positionX) {
        getState().positionX = positionX;
        getState().centered = false;
    }

    /**
     * Gets the distance of Window top border in pixels from top border of the containing (main subwindow) when the subwindow is in
     * {@link SubWindowMode#NORMAL} state, or when next set to that state.
     *
     * @return Distance of Window top border in pixels from top border of the containing (main subwindow). or -1 if unspecified
     *
     * @since 4.0.0
     */
    public int getPositionY() {
        return getState(false).positionY;
    }

    /**
     * Returns the position of this subwindow in the order of all open subwindows for this UI.
     * <p>
     * Window with position 0 is on the bottom, and subwindow with greatest position is at the top. If subwindow has no position (it's not yet
     * attached or hidden) then position is {@code -1}.
     *
     * @see UI#addWindowOrderUpdateListener(com.vaadin.ui.UI.WindowOrderUpdateListener)
     *
     * @since 8.0
     *
     * @return subwindow order position.
     */
    public int getOrderPosition() {
        return orderPosition;
    }

    /**
     * Sets the distance of Window top border in pixels from top border of the containing (main subwindow). Has effect only if in
     * {@link SubWindowMode#NORMAL} mode.
     *
     * @param positionY the Distance of Window top border in pixels from top border of the containing (main subwindow). or -1 if unspecified
     *
     * @since 4.0.0
     */
    public void setPositionY(int positionY) {
        getState().positionY = positionY;
        getState().centered = false;
    }

    private static final Method WINDOW_CLOSE_METHOD;

    static {
        try {
            WINDOW_CLOSE_METHOD = CloseListener.class
                    .getDeclaredMethod("windowClose", CloseEvent.class);
        } catch (final NoSuchMethodException e) {
            // This should never happen
            throw new RuntimeException(
                    "Internal error, window close method not found");
        }
    }

    /**
     *
     */
    public static class CloseEvent extends Component.Event {

        /**
         *
         * @param source source
         */
        public CloseEvent(Component source) {
            super(source);
        }

        /**
         * Gets the Window.
         *
         * @return the subwindow.
         */
        public SubWindow getWindow() {
            return (SubWindow) getSource();
        }
    }

    /**
     * Event which is fired when the subwindow order position is changed.
     *
     * @see UI.WindowOrderUpdateEvent
     *
     * @author Vaadin Ltd
     *
     */
    public static class WindowOrderChangeEvent extends Component.Event {

        private final int order;

        /**
         *
         * @param source
         * @param order
         */
        public WindowOrderChangeEvent(Component source, int order) {
            super(source);
            this.order = order;
            LOGGER.log(Level.INFO, "order: " + order);
        }

        /**
         * Gets the Window.
         *
         * @return the subwindow
         */
        public SubWindow getWindow() {
            return (SubWindow) getSource();
        }

        /**
         * Gets the new subwindow order position.
         *
         * @return the new order position
         */
        public int getOrder() {
            return order;
        }
    }

    /**
     * An interface used for listening to Window order change events.
     *
     * @see UI.WindowOrderUpdateListener
     */
    @FunctionalInterface
    public interface WindowOrderChangeListener extends ConnectorEventListener {

        /**
         *
         */
        public static final Method windowOrderChangeMethod = ReflectTools
                .findMethod(WindowOrderChangeListener.class,
                        "windowOrderChanged", WindowOrderChangeEvent.class);

        /**
         * Called when the subwindow order position is changed. Use {@link WindowOrderChangeEvent#getWindow()} to get a reference to the
         * {@link SubWindow} whose order position is changed. Use {@link WindowOrderChangeEvent#getOrder()} to get a new order position.
         *
         * @param event
         */
        public void windowOrderChanged(WindowOrderChangeEvent event);
    }

    /**
     * Adds a WindowOrderChangeListener to the subwindow.
     * <p>
     * The WindowOrderChangeEvent is fired when the order position is changed. It can happen when some subwindow (this or other) is brought to front
     * or detached.
     * <p>
     * The other way to listen positions of all subwindows in UI is
     * {@link UI#addWindowOrderUpdateListener(com.vaadin.ui.UI.WindowOrderUpdateListener)}
     *
     * @return registration
     * @see UI#addWindowOrderUpdateListener(com.vaadin.ui.UI.WindowOrderUpdateListener)
     *
     * @param listener the WindowModeChangeListener to add.
     * @since 8.0
     */
    public Registration addWindowOrderChangeListener(WindowOrderChangeListener listener) {
        addListener(EventId.WINDOW_ORDER, WindowOrderChangeEvent.class,
                listener, WindowOrderChangeListener.windowOrderChangeMethod);
        return () -> removeListener(EventId.WINDOW_ORDER, WindowOrderChangeEvent.class, listener);
    }

    /**
     *
     * @param order order
     */
    protected void fireWindowOrderChange(Integer order) {
        if (order == null || orderPosition != order) {
            orderPosition = (order == null) ? -1 : order;
            fireEvent(new SubWindow.WindowOrderChangeEvent(this,
                    getOrderPosition()));
        }
    }

    /**
     * An interface used for listening to Window close events. Add the CloseListener to a subwindow and {@link CloseListener#windowClose(CloseEvent)}
     * will be called whenever the user closes the window.
     *
     */
    @FunctionalInterface
    public interface CloseListener extends Serializable {

        /**
         * Called when the user closes a subwindow. Use {@link CloseEvent#getWindow()} to get a reference to the {@link SubWindow} that was closed.
         *
         * @param e The triggered event
         */
        public void windowClose(CloseEvent e);
    }

    /**
     * Adds a CloseListener to the subwindow.
     *
     * For a subwindow the CloseListener is fired when the user closes it (clicks on the close button).
     *
     * For a browser level subwindow the CloseListener is fired when the browser level subwindow is closed. Note that closing a browser level
     * subwindow does not mean it will be destroyed. Also note that Opera does not send events like all other browsers and therefore the close
     * listener might not be called if Opera is used.
     *
     * <p>
     * Since Vaadin 6.5, removing subwindows using #removeWindow(SubWindow) does fire the CloseListener.
     * </p>
     *
     * @param listener the CloseListener to add, not null
     * @return Registration
     * @since 8.0
     */
    public Registration addCloseListener(CloseListener listener) {
        return addListener(CloseEvent.class, listener, WINDOW_CLOSE_METHOD);
    }

    /**
     * Removes the CloseListener from the subwindow.
     *
     * <p>
     * For more information on CloseListeners see {@link CloseListener}.
     * </p>
     *
     * @param listener the CloseListener to remove.
     */
    @Deprecated
    public void removeCloseListener(CloseListener listener) {
        removeListener(CloseEvent.class, listener, WINDOW_CLOSE_METHOD);
    }

    /**
     *
     */
    protected void fireClose() {
        fireEvent(new SubWindow.CloseEvent(this));
    }

    /**
     * Event which is fired when the mode of the Window changes.
     *
     * @author Vaadin Ltd
     * @since 7.1
     *
     */
    public static class WindowModeChangeEvent extends Component.Event {

        private final SubWindowMode windowMode;

        /**
         *
         * @param source
         * @param windowMode
         */
        public WindowModeChangeEvent(Component source, SubWindowMode windowMode) {
            super(source);
            this.windowMode = windowMode;
        }

        /**
         * Gets the Window.
         *
         * @return the subwindow
         */
        public SubWindow getWindow() {
            return (SubWindow) getSource();
        }

        /**
         * Gets the new subwindow mode.
         *
         * @return the new mode
         */
        public SubWindowMode getWindowMode() {
            return windowMode;
        }
    }

    /**
     * An interface used for listening to Window maximize / restore events. Add the WindowModeChangeListener to a subwindow and
     * {@link WindowModeChangeListener#windowModeChanged(WindowModeChangeEvent)} will be called whenever the subwindow is maximized (
     * {@link SubWindowMode#MAXIMIZED}) or restored ({@link SubWindowMode#NORMAL} ).
     */
    @FunctionalInterface
    public interface WindowModeChangeListener extends Serializable {

        /**
         *
         */
        public static final Method windowModeChangeMethod = ReflectTools
                .findMethod(WindowModeChangeListener.class, "windowModeChanged",
                        WindowModeChangeEvent.class);

        /**
         * Called when the user maximizes / restores a subwindow. Use {@link WindowModeChangeEvent#getWindow()} to get a reference to the
         * {@link SubWindow} that was maximized / restored. Use {@link WindowModeChangeEvent#getWindowMode()} to get a reference to the new state.
         *
         * @param event
         */
        public void windowModeChanged(WindowModeChangeEvent event);
    }

    /**
     * Adds a WindowModeChangeListener to the subwindow.
     *
     * The WindowModeChangeEvent is fired when the user changed the display state by clicking the maximize/restore button or by double clicking on the
     * subwindow header. The event is also fired if the state is changed using #setWindowMode(WindowMode).
     *
     * @param listener the WindowModeChangeListener to add.
     * @return Registration
     * @since 8.0
     */
    public Registration addWindowModeChangeListener(
            WindowModeChangeListener listener) {
        return addListener(WindowModeChangeEvent.class, listener,
                WindowModeChangeListener.windowModeChangeMethod);
    }

    /**
     * Removes the WindowModeChangeListener from the subwindow.
     *
     * @param listener the WindowModeChangeListener to remove.
     */
    @Deprecated
    public void removeWindowModeChangeListener(
            WindowModeChangeListener listener) {
        removeListener(WindowModeChangeEvent.class, listener,
                WindowModeChangeListener.windowModeChangeMethod);
    }

    /**
     *
     */
    protected void fireWindowWindowModeChange() {
        fireEvent(
                new SubWindow.WindowModeChangeEvent(this, getState().windowMode));
    }

    /**
     * Method for the resize event.
     */
    private static final Method WINDOW_RESIZE_METHOD;

    static {
        try {
            WINDOW_RESIZE_METHOD = ResizeListener.class
                    .getDeclaredMethod("windowResized", ResizeEvent.class);
        } catch (final NoSuchMethodException e) {
            // This should never happen
            throw new RuntimeException(
                    "Internal error, window resized method not found");
        }
    }

    /**
     * Resize events are fired whenever the client-side fires a resize-event (e.g. the browser subwindow is resized). The frequency may vary across
     * browsers.
     */
    public static class ResizeEvent extends Component.Event {

        /**
         *
         * @param source
         */
        public ResizeEvent(Component source) {
            super(source);
        }

        /**
         * Get the subwindow form which this event originated.
         *
         * @return the subwindow
         */
        public SubWindow getWindow() {
            return (SubWindow) getSource();
        }
    }

    /**
     * Listener for subwindow resize events.
     *
     * @see com.vaadin.ui.Window.ResizeEvent
     */
    @FunctionalInterface
    public interface ResizeListener extends Serializable {

        /**
         *
         * @param e
         */
        public void windowResized(ResizeEvent e);
    }

    /**
     * Add a resize listener.
     *
     * @see Registration
     *
     * @param listener the listener to add, not null
     * @return a registration object for removing the listener
     * @since 8.0
     */
    public Registration addResizeListener(ResizeListener listener) {
        return addListener(ResizeEvent.class, listener, WINDOW_RESIZE_METHOD);
    }

    /**
     * Remove a resize listener.
     *
     * @param listener listener
     */
    @Deprecated
    public void removeResizeListener(ResizeListener listener) {
        removeListener(ResizeEvent.class, listener);
    }

    /**
     * Fire the resize event.
     */
    protected void fireResize() {
        fireEvent(new ResizeEvent(this));
    }

    /**
     * Used to keep the right order of subwindows if multiple subwindows are brought to front in a single changeset. If this is not used, the order is
     * quite random (depends on the order getting to dirty list. e.g. which subwindow got variable changes).
     */
    private Integer bringToFront = null;

    /**
     * If there are currently several subwindows visible, calling this method makes this subwindow topmost.
     *
     * This method can only be called if this subwindow connected a UI. Else an illegal state exception is thrown. Also if there are modal subwindows
     * and this subwindow is not modal, and illegal state exception is thrown.
     */
    public void bringToFront() {
        UI uI = getUI();
        if (uI == null) {
            throw new IllegalStateException(
                    "Window must be attached to parent before calling bringToFront method.");
        }

        if ((bringToFront != null) && (bringToFront >= ((WindowsDesktopArea) getParent()).getWindows().size() - 1)) {
            return;
        }
        int maxBringToFront = -1;
        for (SubWindow w : ((WindowsDesktopArea) getParent().getParent()).getWindows()) {
            if (!isModal() && w.isModal()) {
                throw new IllegalStateException(
                        "The UI contains modal windows, non-modal window cannot be brought to front.");
            }
            if (w.bringToFront != null) {
//                maxBringToFront = Math.max(maxBringToFront,
//                        w.forceBringToFront.intValue());
                w.bringToFront = (w.bringToFront != 0 && w.bringToFront != null ? w.bringToFront-- : 0);
            }
            if (w != this) {
                w.getState().forceBringToFront = 0;
            }
        }

//        forceBringToFront = Integer.valueOf(maxBringToFront + 1);
        bringToFront = ((WindowsDesktopArea) getParent().getParent()).getWindows().size() - 1;

        this.getState().forceBringToFront++;
        markAsDirty();

    }

    /**
     * Sets subwindow modality. When a modal subwindow is open, components outside that subwindow cannot be accessed.
     * <p>
     * Keyboard navigation is restricted by blocking the tab key at the top and bottom of the subwindow by activating the tab stop function
     * internally.
     *
     * @param modal true if modality is to be turned on
     */
    public void setModal(boolean modal) {
        getState().modal = modal;
        center();
    }

    /**
     * @return true if this subwindow is modal.
     */
    public boolean isModal() {
        return getState(false).modal;
    }

    /**
     * Sets subwindow resizable.
     *
     * @param resizable true if resizability is to be turned on
     */
    public void setResizable(boolean resizable) {
        getState().resizable = resizable;
    }

    /**
     *
     * @return true if subwindow is resizable by the end-user, otherwise false.
     */
    public boolean isResizable() {
        return getState(false).resizable;
    }

    /**
     *
     * @return true if a delay is used before recalculating sizes, false if sizes are recalculated immediately.
     */
    public boolean isResizeLazy() {
        return getState(false).resizeLazy;
    }

    /**
     * Should resize operations be lazy, i.e. should there be a delay before layout sizes are recalculated. Speeds up resize operations in slow UIs
     * with the penalty of slightly decreased usability.
     *
     * Note, some browser send false resize events for the browser subwindow and are therefore always lazy.
     *
     * @param resizeLazy true to use a delay before recalculating sizes, false to calculate immediately.
     */
    public void setResizeLazy(boolean resizeLazy) {
        getState().resizeLazy = resizeLazy;
    }

    /**
     * Sets this subwindow to be centered relative to its parent subwindow. Affects subwindows only. If the subwindow is resized as a result of the
     * size of its content changing, it will keep itself centered as long as its position is not explicitly changed programmatically or by the user.
     * <p>
     * <b>NOTE:</b> This method has several issues as currently implemented. Please refer to http://dev.vaadin.com/ticket/8971 for details.
     */
    public void center() {
        getState().centered = true;
    }

    /**
     * Returns the closable status of the subwindow. If a subwindow is closable, it typically shows an X in the upper right corner. Clicking on the X
     * sends a close event to the server. Setting closable to false will remove the X from the subwindow and prevent the user from closing the
     * subwindow.
     *
     * @return true if the subwindow can be closed by the user.
     */
    public boolean isClosable() {
        return getState(false).closable;
    }

    /**
     * Sets the closable status for the subwindow. If a subwindow is closable it typically shows an X in the upper right corner. Clicking on the X
     * sends a close event to the server. Setting closable to false will remove the X from the subwindow and prevent the user from closing the
     * subwindow.
     *
     * @param closable determines if the subwindow can be closed by the user.
     */
    public void setClosable(boolean closable) {
        if (closable != isClosable()) {
            getState().closable = closable;
        }
    }

    /**
     * Indicates whether a subwindow can be dragged or not. By default a subwindow is draggable.
     *
     * @return {@code true} if subwindow is draggable; {@code false} if not
     */
    public boolean isDraggable() {
        return getState(false).draggable;
    }

    /**
     * Enables or disables that a subwindow can be dragged (moved) by the user. By default a subwindow is draggable.
     *
     * @param draggable true if the subwindow can be dragged by the user
     */
    public void setDraggable(boolean draggable) {
        getState().draggable = draggable;
    }

    /**
     * Gets the current mode of the subwindow.
     *
     * @see SubWindowMode
     * @return the mode of the subwindow.
     */
    public SubWindowMode getWindowMode() {
        return getState(false).windowMode;
    }

    /**
     * Sets the mode for the subwindow.
     *
     * @param windowMode windowMode
     * @see SubWindowMode
     */
    public void setWindowMode(SubWindowMode windowMode) {
        if (windowMode != getWindowMode()) {
            getState().windowMode = windowMode;
            fireWindowWindowModeChange();
        }
    }

    /**
     * This is the old way of adding a keyboard shortcut to close a {@link SubWindow} - to preserve compatibility with existing code under the new
     * functionality, this method now first removes all registered close shortcuts, then adds the default ESCAPE shortcut key, and then attempts to
     * add the shortcut provided as parameters to this method. This method, and its companion {@link #removeCloseShortcut()}, are now considered
     * deprecated, as their main function is to preserve exact backwards compatibility with old code. For all new code, use the new keyboard shortcuts
     * API: {@link #addCloseShortcut(int,int...)},
     * {@link #removeCloseShortcut(int,int...)},
     * {@link #removeAllCloseShortcuts()}, {@link #hasCloseShortcut(int,int...)} and {@link #getCloseShortcuts()}.
     * <p>
     * Original description: Makes it possible to close the subwindow by pressing the given {@link KeyCode} and (optional) {@link ModifierKey}s. Note
     * that this shortcut only reacts while the subwindow has focus, closing itself - if you want to close a subwindow from a UI, use
     * {@link UI#addAction(com.vaadin.event.Action)} of the UI instead.
     *
     * @param keyCode the keycode for invoking the shortcut
     * @param modifiers the (optional) modifiers for invoking the shortcut. Can be set to null to be explicit about not having modifiers.
     *
     * @deprecated Use {@link #addCloseShortcut(int, int...)} instead.
     */
    @Deprecated
    public void setCloseShortcut(int keyCode, int... modifiers) {
        removeCloseShortcut();
        addCloseShortcut(keyCode, modifiers);
    }

    /**
     * Removes all keyboard shortcuts previously set with {@link #setCloseShortcut(int, int...)} and {@link #addCloseShortcut(int, int...)}, then adds
     * the default {@link KeyCode#ESCAPE} shortcut.
     * <p>
     * This is the old way of removing the (single) keyboard close shortcut, and is retained only for exact backwards compatibility. For all new code,
     * use the new keyboard shortcuts API: {@link #addCloseShortcut(int,int...)},
     * {@link #removeCloseShortcut(int,int...)},
     * {@link #removeAllCloseShortcuts()}, {@link #hasCloseShortcut(int,int...)} and {@link #getCloseShortcuts()}.
     *
     * @deprecated Use {@link #removeCloseShortcut(int, int...)} instead.
     */
    @Deprecated
    public void removeCloseShortcut() {
        for (CloseShortcut sc : closeShortcuts) {
            removeAction(sc);
        }
        closeShortcuts.clear();
        addCloseShortcut(KeyCode.ESCAPE);
    }

    /**
     * Adds a close shortcut - pressing this key while holding down all (if any) modifiers specified while this Window is in focus will close the
     * Window.
     *
     * @since 7.6
     * @param keyCode the keycode for invoking the shortcut
     * @param modifiers the (optional) modifiers for invoking the shortcut. Can be set to null to be explicit about not having modifiers.
     */
    public void addCloseShortcut(int keyCode, int... modifiers) {

        // Ignore attempts to re-add existing shortcuts
        if (hasCloseShortcut(keyCode, modifiers)) {
            return;
        }

        // Actually add the shortcut
        CloseShortcut shortcut = new CloseShortcut(this, keyCode, modifiers);
        addAction(shortcut);
        closeShortcuts.add(shortcut);
    }

    /**
     * Removes a close shortcut previously added with {@link #addCloseShortcut(int, int...)}.
     *
     * @since 7.6
     * @param keyCode the keycode for invoking the shortcut
     * @param modifiers the (optional) modifiers for invoking the shortcut. Can be set to null to be explicit about not having modifiers.
     */
    public void removeCloseShortcut(int keyCode, int... modifiers) {
        for (CloseShortcut shortcut : closeShortcuts) {
            if (shortcut.equals(keyCode, modifiers)) {
                removeAction(shortcut);
                closeShortcuts.remove(shortcut);
                return;
            }
        }
    }

    /**
     * Removes all close shortcuts. This includes the default ESCAPE shortcut. It is up to the user to add back any and all keyboard close shortcuts
     * they may require. For more fine-grained control over shortcuts, use {@link #removeCloseShortcut(int, int...)}.
     *
     * @since 7.6
     */
    public void removeAllCloseShortcuts() {
        for (CloseShortcut shortcut : closeShortcuts) {
            removeAction(shortcut);
        }
        closeShortcuts.clear();
    }

    /**
     * Checks if a close subwindow shortcut key has already been registered.
     *
     * @since 7.6
     * @param keyCode the keycode for invoking the shortcut
     * @param modifiers the (optional) modifiers for invoking the shortcut. Can be set to null to be explicit about not having modifiers.
     * @return true, if an exactly matching shortcut has been registered.
     */
    public boolean hasCloseShortcut(int keyCode, int... modifiers) {
        for (CloseShortcut shortcut : closeShortcuts) {
            if (shortcut.equals(keyCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable collection of {@link CloseShortcut} objects currently registered with this {@link SubWindow}. This method is provided
     * mainly so that users can implement their own serialization routines. To check if a certain combination of keys has been registered as a close
     * shortcut, use the {@link #hasCloseShortcut(int, int...)} method instead.
     *
     * @since 7.6
     * @return an unmodifiable Collection of CloseShortcut objects.
     */
    public Collection<CloseShortcut> getCloseShortcuts() {
        return Collections.unmodifiableCollection(closeShortcuts);
    }

    /**
     * A {@link ShortcutListener} specifically made to define a keyboard shortcut that closes the subwindow.
     *
     * <pre>
     * <code>
     *  // within the subwindow using helper
     *  subwindow.setCloseShortcut(KeyCode.ESCAPE, null);
     *
     *  // or globally
     *  getUI().addAction(new Window.CloseShortcut(subwindow, KeyCode.ESCAPE));
     * </code>
     * </pre>
     *
     */
    public static class CloseShortcut extends ShortcutListener {

        /**
         *
         */
        protected SubWindow window;

        /**
         * Creates a keyboard shortcut for closing the given subwindow using the shorthand notation defined in {@link ShortcutAction}.
         *
         * @param window
         * @param shorthandCaption the caption with shortcut keycode and modifiers indicated
         */
        public CloseShortcut(SubWindow window, String shorthandCaption) {
            super(shorthandCaption);
            this.window = window;
        }

        /**
         * Creates a keyboard shortcut for closing the given subwindow using the given {@link KeyCode} and {@link ModifierKey}s.
         *
         * @param window
         * @param keyCode KeyCode to react to
         * @param modifiers optional modifiers for shortcut
         */
        public CloseShortcut(SubWindow window, int keyCode, int... modifiers) {
            super(null, keyCode, modifiers);
            this.window = window;
        }

        /**
         * Creates a keyboard shortcut for closing the given subwindow using the given {@link KeyCode}.
         *
         * @param window
         * @param keyCode KeyCode to react to
         */
        public CloseShortcut(SubWindow window, int keyCode) {
            this(window, keyCode, null);
        }

        /**
         *
         * @param sender sender
         * @param target target
         */
        @Override
        public void handleAction(Object sender, Object target) {
            if (window.isClosable()) {
                window.close();
            }
        }

        /**
         *
         * @param keyCode keyCode
         * @param modifiers modifiers
         * @return
         */
        public boolean equals(int keyCode, int... modifiers) {
            if (keyCode != getKeyCode()) {
                return false;
            }

            if (getModifiers() != null) {
                int[] mods = null;
                if (modifiers != null) {
                    // Modifiers provided by the parent ShortcutAction class
                    // are guaranteed to be sorted. We still need to sort
                    // the modifiers passed in as argument.
                    mods = Arrays.copyOf(modifiers, modifiers.length);
                    Arrays.sort(mods);
                }
                return Arrays.equals(mods, getModifiers());
            }
            return true;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.vaadin.event.FieldEvents.FocusNotifier#addFocusListener(com.vaadin
     * .event.FieldEvents.FocusListener)
     */
    @Override
    public Registration addFocusListener(FocusListener listener) {
        return addListener(FocusEvent.EVENT_ID, FocusEvent.class, listener,
                FocusListener.focusMethod);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.vaadin.event.FieldEvents.BlurNotifier#addBlurListener(com.vaadin.
     * event.FieldEvents.BlurListener)
     */
    @Override
    public Registration addBlurListener(BlurListener listener) {
        return addListener(BlurEvent.EVENT_ID, BlurEvent.class, listener,
                BlurListener.blurMethod);
    }

    /**
     * {@inheritDoc}
     *
     * Cause the subwindow to be brought on top of other subwindows and gain keyboard focus.
     */
    @Override
    public void focus() {
        /*
         * When focusing a subwindow it basically means it should be brought to the
         * front. Instead of just moving the keyboard focus we focus the subwindow
         * and bring it top-most.
         */
        super.focus();
        bringToFront();
    }

    @Override
    protected SubWindowState getState() {
        return (SubWindowState) super.getState();
    }

    @Override
    protected SubWindowState getState(boolean markAsDirty) {
        return (SubWindowState) super.getState(markAsDirty);
    }

    /**
     * Allows to specify which components contain the description for the subwindow. Text contained in these components will be read by assistive
     * devices when it is opened.
     *
     * @param components the components to use as description
     */
    public void setAssistiveDescription(Component... components) {
        if (components == null) {
            throw new IllegalArgumentException(
                    "Parameter connectors must be non-null");
        } else {
            getState().contentDescription = components;
        }
    }

    /**
     * Gets the components that are used as assistive description. Text contained in these components will be read by assistive devices when the
     * subwindow is opened.
     *
     * @return array of previously set components
     */
    public Component[] getAssistiveDescription() {
        Connector[] contentDescription = getState(false).contentDescription;
        if (contentDescription == null) {
            return null;
        }

        Component[] target = new Component[contentDescription.length];
        System.arraycopy(contentDescription, 0, target, 0,
                contentDescription.length);

        return target;
    }

    /**
     * Sets the accessibility prefix for the subwindow caption.
     *
     * This prefix is read to assistive device users before the subwindow caption, but not visible on the page.
     *
     * @param prefix String that is placed before the subwindow caption
     */
    public void setAssistivePrefix(String prefix) {
        getState().assistivePrefix = prefix;
    }

    /**
     * Gets the accessibility prefix for the subwindow caption.
     *
     * This prefix is read to assistive device users before the subwindow caption, but not visible on the page.
     *
     * @return The accessibility prefix
     */
    public String getAssistivePrefix() {
        return getState(false).assistivePrefix;
    }

    /**
     * Sets the accessibility postfix for the subwindow caption.
     *
     * This postfix is read to assistive device users after the subwindow caption, but not visible on the page.
     *
     * @param assistivePostfix String that is placed after the subwindow caption
     */
    public void setAssistivePostfix(String assistivePostfix) {
        getState().assistivePostfix = assistivePostfix;
    }

    /**
     * Gets the accessibility postfix for the subwindow caption.
     *
     * This postfix is read to assistive device users after the subwindow caption, but not visible on the page.
     *
     * @return The accessibility postfix
     */
    public String getAssistivePostfix() {
        return getState(false).assistivePostfix;
    }

    /**
     * Sets the WAI-ARIA role the subwindow.
     *
     * This role defines how an assistive device handles a subwindow. Available roles are alertdialog and dialog (@see
     * <a href="http://www.w3.org/TR/2011/CR-wai-aria-20110118/roles">Roles Model</a>).
     *
     * The default role is dialog.
     *
     * @param role WAI-ARIA role to set for the subwindow
     */
    public void setAssistiveRole(WindowRole role) {
        getState().role = role;
    }

    /**
     * Gets the WAI-ARIA role the subwindow.
     *
     * This role defines how an assistive device handles a subwindow. Available roles are alertdialog and dialog (@see
     * <a href="http://www.w3.org/TR/2011/CR-wai-aria-20110118/roles">Roles Model</a>).
     *
     * @return WAI-ARIA role set for the subwindow
     */
    public WindowRole getAssistiveRole() {
        return getState(false).role;
    }

    /**
     * Set if it should be prevented to set the focus to a component outside a non-modal subwindow with the tab key.
     * <p>
     * This is meant to help users of assistive devices to not leaving the subwindow unintentionally.
     * <p>
     * For modal subwindows, this function is activated automatically, while preserving the stored value of tabStop.
     *
     * @param tabStop true to keep the focus inside the subwindow when reaching the top or bottom, false (default) to allow leaving the subwindow
     */
    public void setTabStopEnabled(boolean tabStop) {
        getState().assistiveTabStop = tabStop;
    }

    /**
     * Get if it is prevented to leave a subwindow with the tab key.
     *
     * @return true when the focus is limited to inside the subwindow, false when focus can leave the subwindow
     */
    public boolean isTabStopEnabled() {
        return getState(false).assistiveTabStop;
    }

    /**
     * Sets the message that is provided to users of assistive devices when the user reaches the top of the subwindow when leaving a subwindow with
     * the tab key is prevented.
     * <p>
     * This message is not visible on the screen.
     *
     * @param topMessage String provided when the user navigates with Shift-Tab keys to the top of the subwindow
     */
    public void setTabStopTopAssistiveText(String topMessage) {
        getState().assistiveTabStopTopText = topMessage;
    }

    /**
     * Sets the message that is provided to users of assistive devices when the user reaches the bottom of the subwindow when leaving a subwindow with
     * the tab key is prevented.
     * <p>
     * This message is not visible on the screen.
     *
     * @param bottomMessage String provided when the user navigates with the Tab key to the bottom of the subwindow
     */
    public void setTabStopBottomAssistiveText(String bottomMessage) {
        getState().assistiveTabStopBottomText = bottomMessage;
    }

    /**
     * Gets the message that is provided to users of assistive devices when the user reaches the top of the subwindow when leaving a subwindow with
     * the tab key is prevented.
     *
     * @return the top message
     */
    public String getTabStopTopAssistiveText() {
        return getState(false).assistiveTabStopTopText;
    }

    /**
     * Gets the message that is provided to users of assistive devices when the user reaches the bottom of the subwindow when leaving a subwindow with
     * the tab key is prevented.
     *
     * @return the bottom message
     */
    public String getTabStopBottomAssistiveText() {
        return getState(false).assistiveTabStopBottomText;
    }

    @Override
    public void readDesign(Element design, DesignContext context) {
        super.readDesign(design, context);

        if (design.hasAttr("center")) {
            center();
        }
        if (design.hasAttr("position")) {
            String[] position = design.attr("position").split(",");
            setPositionX(Integer.parseInt(position[0]));
            setPositionY(Integer.parseInt(position[1]));
        }

        // Parse shortcuts if defined, otherwise rely on default behavior
        if (design.hasAttr("close-shortcut")) {

            // Parse shortcuts
            String[] shortcutStrings = DesignAttributeHandler
                    .readAttribute("close-shortcut", design.attributes(),
                            String.class)
                    .split("\\s+");

            removeAllCloseShortcuts();

            for (String part : shortcutStrings) {
                if (!part.isEmpty()) {
                    ShortcutAction shortcut = DesignAttributeHandler
                            .getFormatter()
                            .parse(part.trim(), ShortcutAction.class);
                    addCloseShortcut(shortcut.getKeyCode(),
                            shortcut.getModifiers());
                }
            }
        }
    }

    /**
     * Reads the content and possible assistive descriptions from the list of child elements of a design. If an element has an
     * {@code :assistive-description} attribute, adds the parsed component to the list of components used as the assistive description of this Window.
     * Otherwise, sets the component as the content of this Window. If there are multiple non-description elements, throws a DesignException.
     *
     * @param children child elements in a design
     * @param context the DesignContext instance used to parse the design
     *
     * @throws DesignException if there are multiple non-description child elements
     * @throws DesignException if a child element could not be parsed as a Component
     *
     * @see #setContent(Component)
     * @see #setAssistiveDescription(Component...)
     */
    @Override
    protected void readDesignChildren(Elements children,
            DesignContext context) {
        List<Component> descriptions = new ArrayList<>();
        Elements content = new Elements();

        for (Element child : children) {
            if (child.hasAttr(":assistive-description")) {
                descriptions.add(context.readDesign(child));
            } else {
                content.add(child);
            }
        }
        super.readDesignChildren(content, context);
        setAssistiveDescription(
                descriptions.toArray(new Component[descriptions.size()]));
    }

    @Override
    public void writeDesign(Element design, DesignContext context) {
        super.writeDesign(design, context);

        SubWindow def = context.getDefaultInstance(this);

        if (getState().centered) {
            design.attr("center", true);
        }

        DesignAttributeHandler.writeAttribute("position", design.attributes(),
                getPosition(), def.getPosition(), String.class, context);

        // Process keyboard shortcuts
        if (closeShortcuts.size() == 1 && hasCloseShortcut(KeyCode.ESCAPE)) {
            // By default, we won't write anything if we're relying on default
            // shortcut behavior
        } else {
            // Dump all close shortcuts to a string...
            String attrString = "";

            // TODO: add canonical support for array data in XML attributes
            for (CloseShortcut shortcut : closeShortcuts) {
                String shortcutString = DesignAttributeHandler.getFormatter()
                        .format(shortcut, CloseShortcut.class);
                attrString += shortcutString + " ";
            }

            // Write everything except the last "," to the design
            DesignAttributeHandler.writeAttribute("close-shortcut",
                    design.attributes(), attrString.trim(), null, String.class,
                    context);
        }

        for (Component c : getAssistiveDescription()) {
            Element child = context.createElement(c)
                    .attr(":assistive-description", true);
            design.appendChild(child);
        }
    }

    private String getPosition() {
        return getPositionX() + "," + getPositionY();
    }

    @Override
    protected Collection<String> getCustomAttributes() {
        Collection<String> result = super.getCustomAttributes();
        result.add("center");
        result.add("position");
        result.add("position-y");
        result.add("position-x");
        result.add("close-shortcut");
        return result;
    }
}
