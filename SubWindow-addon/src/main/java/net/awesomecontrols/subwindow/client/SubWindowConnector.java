package net.awesomecontrols.subwindow.client;

import net.awesomecontrols.subwindow.SubWindow;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.LayoutManager;
import com.vaadin.client.Paintable;
import com.vaadin.client.UIDL;
import com.vaadin.client.communication.StateChangeEvent;
import static com.vaadin.client.ui.AbstractComponentConnector.isRealUpdate;
import com.vaadin.client.ui.AbstractSingleComponentContainerConnector;
import com.vaadin.client.ui.ClickEventHandler;
import com.vaadin.client.ui.PostLayoutListener;
import com.vaadin.client.ui.ShortcutActionHandler;
import com.vaadin.client.ui.SimpleManagedLayout;
import com.vaadin.client.ui.VNotification;
import com.vaadin.client.ui.VWindow;
import com.vaadin.client.ui.layout.MayScrollChildren;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.window.WindowServerRpc;
import java.util.logging.Logger;

// Connector binds client-side widget class to server-side component class
// Connector lives in the client and the @Connect annotation specifies the
// corresponding server-side component

/**
 *
 * @author SShadow
 */
@Connect(SubWindow.class)
public class SubWindowConnector extends AbstractSingleComponentContainerConnector
        implements Paintable, SimpleManagedLayout, PostLayoutListener,
        MayScrollChildren, SubWindowMoveHandler {

    private Node windowClone;

    private ClickEventHandler clickEventHandler = new ClickEventHandler(this) {
        @Override
        protected void fireClick(NativeEvent event,
                MouseEventDetails mouseDetails) {
            getRpcProxy(WindowServerRpc.class).click(mouseDetails);
        }
    };

    abstract class WindowEventHandler
            implements ClickHandler, DoubleClickHandler {
    }

    private WindowEventHandler maximizeRestoreClickHandler = new WindowEventHandler() {

        @Override
        public void onClick(ClickEvent event) {
            final Element target = event.getNativeEvent().getEventTarget()
                    .cast();
            if (target == getWidget().maximizeRestoreBox) {
                // Click on maximize/restore box
                onMaximizeRestore();
            }
        }

        @Override
        public void onDoubleClick(DoubleClickEvent event) {
            final Element target = event.getNativeEvent().getEventTarget()
                    .cast();
            if (getWidget().header.isOrHasChild(target)) {
                // Double click on header
                onMaximizeRestore();
            }
        }
    };

    @Override
    public boolean delegateCaptionHandling() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        SubWindowWidget window = getWidget();
        window.id = getConnectorId();
        window.client = getConnection();
        window.connector = this;

        getLayoutManager().registerDependency(this,
                window.contentPanel.getElement());
        getLayoutManager().registerDependency(this, window.header);
        getLayoutManager().registerDependency(this, window.footer);

        window.addHandler(maximizeRestoreClickHandler, ClickEvent.getType());
        window.addHandler(maximizeRestoreClickHandler,
                DoubleClickEvent.getType());

        window.setOwner(getConnection().getUIConnector().getWidget());

        window.addMoveHandler(this);
        
        // MODIFICADO 
        this.setWindowOrderAndPosition();
    }

    @Override
    public void onUnregister() {
        LayoutManager lm = getLayoutManager();
        SubWindowWidget window = getWidget();
        lm.unregisterDependency(this, window.contentPanel.getElement());
        lm.unregisterDependency(this, window.header);
        lm.unregisterDependency(this, window.footer);
    }

    /**
     *
     * @param uidl uidl
     * @param client client
     */
    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {

        SubWindowWidget window = getWidget();
        String connectorId = getConnectorId();

        // Workaround needed for Testing Tools (GWT generates window DOM
        // slightly different in different browsers).
        window.closeBox.setId(connectorId + "_window_close");
        window.maximizeRestoreBox
                .setId(connectorId + "_window_maximizerestore");

        window.visibilityChangesDisabled = true;
        if (!isRealUpdate(uidl)) {
            return;
        }
        window.visibilityChangesDisabled = false;

        // we may have actions
        for (int i = 0; i < uidl.getChildCount(); i++) {
            UIDL childUidl = uidl.getChildUIDL(i);
            if (childUidl.getTag().equals("actions")) {
                if (window.shortcutHandler == null) {
                    window.shortcutHandler = new ShortcutActionHandler(
                            connectorId, client);
                }
                window.shortcutHandler.updateActionMap(childUidl);
            }

        }

        if (uidl.hasAttribute("bringToFront")) {
            /*
             * Focus as a side-effect. Will be overridden by
             * ApplicationConnection if another component was focused by the
             * server side.
             */
            window.contentPanel.focus();
            window.bringToFrontSequence = uidl.getIntAttribute("bringToFront");
            VWindow.deferOrdering();
        }
    }

    @Override
    public void updateCaption(ComponentConnector component) {
        // NOP, window has own caption, layout caption not rendered
    }

    @Override
    public SubWindowWidget getWidget() {
        return (SubWindowWidget) super.getWidget();
    }

    @Override
    public void onConnectorHierarchyChange(
            ConnectorHierarchyChangeEvent event) {
        // We always have 1 child, unless the child is hidden
        getWidget().contentPanel.setWidget(getContentWidget());

        if (getParent() == null && windowClone != null) {
            // If the window is removed from the UI, add the copy of the
            // contents to the window (in case of an 'out-animation')
            getWidget().getElement().removeAllChildren();
            getWidget().getElement().appendChild(windowClone);

            // Clean reference
            windowClone = null;
        }

    }

    /**
     *
     */
    @Override
    public void layout() {
        LayoutManager lm = getLayoutManager();
        SubWindowWidget window = getWidget();

        // ensure window is not larger than browser window
        if (window.getOffsetWidth() > Window.getClientWidth()) {
            window.setWidth(Window.getClientWidth() + "px");
            lm.setNeedsMeasure(getContent());
        }
        if (window.getOffsetHeight() > Window.getClientHeight()) {
            window.setHeight(Window.getClientHeight() + "px");
            lm.setNeedsMeasure(getContent());
        }

        ComponentConnector content = getContent();
        boolean hasContent = content != null;
        Element contentElement = window.contentPanel.getElement();

        Style contentStyle = window.contents.getStyle();

        int headerHeight = lm.getOuterHeight(window.header);
        contentStyle.setPaddingTop(headerHeight, Style.Unit.PX);
        contentStyle.setMarginTop(-headerHeight, Style.Unit.PX);

        int footerHeight = lm.getOuterHeight(window.footer);
        contentStyle.setPaddingBottom(footerHeight, Style.Unit.PX);
        contentStyle.setMarginBottom(-footerHeight, Style.Unit.PX);

        int minWidth = lm.getOuterWidth(window.header)
                - lm.getInnerWidth(window.header);
        int minHeight = footerHeight + headerHeight;

        getWidget().getElement().getStyle().setPropertyPx("minWidth", minWidth);
        getWidget().getElement().getStyle().setPropertyPx("minHeight",
                minHeight);

        /*
         * Must set absolute position if the child has relative height and
         * there's a chance of horizontal scrolling as some browsers will
         * otherwise not take the scrollbar into account when calculating the
         * height.
         */
        if (hasContent) {
            Element layoutElement = content.getWidget().getElement();
            Style childStyle = layoutElement.getStyle();

            if (content.isRelativeHeight()) {
                childStyle.setPosition(Style.Position.ABSOLUTE);

                Style wrapperStyle = contentElement.getStyle();
                if (window.getElement().getStyle().getWidth().isEmpty()
                        && !content.isRelativeWidth()) {
                    /*
                     * Need to lock width to make undefined width work even with
                     * absolute positioning
                     */
                    int contentWidth = lm.getOuterWidth(layoutElement);
                    wrapperStyle.setWidth(contentWidth, Style.Unit.PX);
                } else {
                    wrapperStyle.clearWidth();
                }
            } else {
                childStyle.clearPosition();
            }
        }

    }

    @Override
    public void postLayout() {
        SubWindowWidget window = getWidget();

        if (!window.isAttached()) {
            Logger.getLogger(SubWindowConnector.class.getName())
                    .warning("Called postLayout to detached Window.");
            return;
        }
        if (window.centered && getState().windowMode != SubWindowMode.MAXIMIZED) {
            window.center();
        }
        window.positionOrSizeUpdated();

        if (getParent() != null) {
            // Take a copy of the contents, since the server will detach all
            // children of this window when it's closed, and the window will be
            // emptied during the following hierarchy update (we need to keep
            // the contents visible for the duration of a possible
            // 'out-animation')

            // Fix for #14645 and #14785 - as soon as we clone audio and video
            // tags, they start fetching data, and playing immediately in
            // background, in case autoplay attribute is present. Therefore we
            // have to replace them with stubs in the clone. And we can't just
            // erase them, because there are corresponding player widgets to
            // animate
            windowClone = cloneNodeFilteringMedia(
                    getWidget().getElement().getFirstChild());
        }
    }

    private Node cloneNodeFilteringMedia(Node node) {
        if (node instanceof Element) {
            Element old = (Element) node;
            if ("audio".equalsIgnoreCase(old.getTagName())
                    || "video".equalsIgnoreCase(old.getTagName())) {
                if (!old.hasAttribute("controls")
                        && "audio".equalsIgnoreCase(old.getTagName())) {
                    // nothing to animate, so we won't add this to
                    // the clone
                    return null;
                }
                Element newEl = DOM.createElement(old.getTagName());
                if (old.hasAttribute("controls")) {
                    newEl.setAttribute("controls",
                            old.getAttribute("controls"));
                }
                if (old.hasAttribute("style")) {
                    newEl.setAttribute("style", old.getAttribute("style"));
                }
                if (old.hasAttribute("class")) {
                    newEl.setAttribute("class", old.getAttribute("class"));
                }
                return newEl;
            }
        }
        Node res = node.cloneNode(false);
        if (node.hasChildNodes()) {
            NodeList<Node> nl = node.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node clone = cloneNodeFilteringMedia(nl.getItem(i));
                if (clone != null) {
                    res.appendChild(clone);
                }
            }
        }
        return res;
    }

    @Override
    public SubWindowState getState() {
        return (SubWindowState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);

        SubWindowWidget window = getWidget();
        SubWindowState state = getState();

        if (state.modal != window.vaadinModality) {
            window.setVaadinModality(!window.vaadinModality);
        }
        boolean resizeable = state.resizable
                && state.windowMode == SubWindowMode.NORMAL;
        window.setResizable(resizeable);

        window.resizeLazy = state.resizeLazy;

        window.setDraggable(
                state.draggable && state.windowMode == SubWindowMode.NORMAL);

        window.updateMaximizeRestoreClassName(state.resizable,
                state.windowMode);

        // Caption must be set before required header size is measured. If
        // the caption attribute is missing the caption should be cleared.
        String iconURL = null;
        if (getIconUri() != null) {
            iconURL = getIconUri();
        }

        window.setAssistivePrefix(state.assistivePrefix);
        window.setAssistivePostfix(state.assistivePostfix);
        window.setCaption(state.caption, iconURL, getState().captionAsHtml);

        window.setWaiAriaRole(getState().role);
        window.setAssistiveDescription(state.contentDescription);

        window.setTabStopEnabled(getState().assistiveTabStop);
        window.setTabStopTopAssistiveText(getState().assistiveTabStopTopText);
        window.setTabStopBottomAssistiveText(
                getState().assistiveTabStopBottomText);

        clickEventHandler.handleEventHandlerRegistration();

        window.setClosable(state.closable);
        // initialize position from state
        updateWindowPosition();

        // setting scrollposition must happen after children is rendered
        window.contentPanel.setScrollPosition(state.scrollTop);
        window.contentPanel.setHorizontalScrollPosition(state.scrollLeft);

        // Center this window on screen if requested
        // This had to be here because we might not know the content size before
        // everything is painted into the window

        // centered is this is unset on move/resize
        window.centered = state.centered;
        // Ensure centering before setting visible (#16486)
        if (window.centered && getState().windowMode != SubWindowMode.MAXIMIZED) {
            Scheduler.get().scheduleFinally(() -> getWidget().center());
        }
        window.setVisible(true);
        if (this.getState().forceBringToFront > 0) {
            window.bringToFront();
        }
    }

    // Need to override default because of window mode
    @Override
    protected void updateComponentSize() {
        if (getState().windowMode == SubWindowMode.NORMAL) {
            super.updateComponentSize();
        } else if (getState().windowMode == SubWindowMode.MAXIMIZED) {
            super.updateComponentSize("100%", "100%");
        }
    }

    /**
     *
     */
    protected void updateWindowPosition() {
        SubWindowWidget window = getWidget();
        SubWindowState state = getState();
        if (state.windowMode == SubWindowMode.NORMAL) {
            // if centered, position handled in postLayout()
            if (!state.centered
                    && (state.positionX >= 0 || state.positionY >= 0)) {
                // If both positions are negative, then
                // setWindowOrderAndPosition has already taken care of
                // positioning the window so it stacks with other windows
                window.setPopupPosition(state.positionX, state.positionY);
            }
        } else if (state.windowMode == SubWindowMode.MAXIMIZED) {
            window.setPopupPositionNoUpdate(0, 0);
        }
    }

    /**
     *
     */
    protected void updateWindowMode() {
        SubWindowWidget window = getWidget();
        SubWindowState state = getState();

        // update draggable on widget
        window.setDraggable(
                state.draggable && state.windowMode == SubWindowMode.NORMAL);
        // update resizable on widget
        window.setResizable(
                state.resizable && state.windowMode == SubWindowMode.NORMAL);
        updateComponentSize();
        updateWindowPosition();
        window.updateMaximizeRestoreClassName(state.resizable,
                state.windowMode);
        window.updateContentsSize();
    }

    /**
     *
     */
    protected void onMaximizeRestore() {
        SubWindowState state = getState();
        if (state.resizable) {
            if (state.windowMode == SubWindowMode.MAXIMIZED) {
                state.windowMode = SubWindowMode.NORMAL;
            } else {
                state.windowMode = SubWindowMode.MAXIMIZED;
            }
            updateWindowMode();

            SubWindowWidget window = getWidget();
            window.bringToFront();

            getRpcProxy(SubWindowServerRpc.class)
                    .windowModeChanged(state.windowMode);
        }
    }

    /**
     * Gives the WindowConnector an order number. As a side effect, moves the
     * window according to its order number so the windows are stacked. This
     * method should be called for each window in the order they should appear.
     */
    public void setWindowOrderAndPosition() {
        getWidget().setWindowOrderAndPosition();
    }

    @Override
    public boolean hasTooltip() {
        /*
         * Tooltip event handler always needed on the window widget to make sure
         * tooltips are properly hidden. (#11448)
         */
        return true;
    }

    @Override
    public void onWindowMove(SubWindowMoveEvent event) {
        getRpcProxy(WindowServerRpc.class).windowMoved(event.getNewX(),
                event.getNewY());
    }
}
