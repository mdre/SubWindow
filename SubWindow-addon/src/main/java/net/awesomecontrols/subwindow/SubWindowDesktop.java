/*
 * Copyright 2017 Marcelo D. Ré {@literal <marcelo.re@gmail.com>}.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.awesomecontrols.subwindow;

import com.vaadin.ui.Button;
import net.awesomecontrols.subwindow.client.SubWindowDesktopState;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class SubWindowDesktop extends Composite implements SubWindow.CloseListener {
    private final static Logger LOGGER = Logger.getLogger(SubWindowDesktop.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }

    private Panel desktopPanel = new Panel();
    private WindowsDesktopArea windowDesktopArea = new WindowsDesktopArea();
    HorizontalLayout openedWindowsBar = new HorizontalLayout();
    /**
     * create an empty desktop
     */
    public SubWindowDesktop() {
        this.init();
    }
    
    private void init() {
        windowDesktopArea.setSizeFull();
        
        openedWindowsBar.setHeight(30,Unit.PIXELS);
        openedWindowsBar.setMargin(false);
        openedWindowsBar.setSpacing(false);
        openedWindowsBar.addStyleName("openedWindowsBar");
        
        VerticalLayout lyDesktop = new VerticalLayout();
        lyDesktop.setSizeFull();
        lyDesktop.setMargin(false);
        lyDesktop.setSpacing(false);
        
        lyDesktop.addComponent(windowDesktopArea);
        lyDesktop.addComponent(openedWindowsBar);
        lyDesktop.setExpandRatio(windowDesktopArea, 1);
        
        desktopPanel.setContent(lyDesktop);
        this.setCompositionRoot(desktopPanel);
    }
    
    /**
     * Add a subwindow to the desktop.
     *
     * @param sw
     *            the subwindow to be added
     * @return 
     */
    public SubWindowDesktop addSubWindow(SubWindow sw) {
        this.windowDesktopArea.addSubWindow(sw);
        
        Button wButton = new Button(sw.getCaption());
        wButton.setData(sw);
        wButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        wButton.addStyleName(ValoTheme.BUTTON_TINY);
        wButton.addClickListener((event) -> {
            SubWindow swd = (SubWindow)event.getButton().getData();
            ((SubWindow)event.getButton().getData()).bringToFront();
        });
        
        
        this.openedWindowsBar.addComponent(wButton);
        sw.addCloseListener(this);
        return this;
    }
    
    /**
     * Hide the opened windows bar
     * @return this
     */
    public SubWindowDesktop hideOpenedWindowsBar() {
        this.openedWindowsBar.setVisible(false);
        return this;
    }
    
    /**
     * Show the opened windows bar
     * @return this
     */
    public SubWindowDesktop showOpenedWindowsBar() {
        this.openedWindowsBar.setVisible(true);
        return this;
    }
    
    /**
     * Set de OpenedWindowsBar height
     * @param h height
     * @param u Unit
     * @return this
     */
    public SubWindowDesktop setOpenedWindowsBarHeight(float h, Unit u) {
        openedWindowsBar.setHeight(h,u);
        return this;
    }
    
    /**
     * Remove the window from the desktop
     *
     * @param window
     *            the window to be removed
     * @return 
     */
    public boolean removeWindow(SubWindow window) {
        
        this.windowDesktopArea.removeWindow(window);
        
        // quitar de la barra
        for (Iterator<Component> iterator = this.openedWindowsBar.iterator(); iterator.hasNext();) {
            Button next = (Button)iterator.next();
            if (next.getData() == window ) {
                this.openedWindowsBar.removeComponent(next);
            }
        }
//        desktopPanel.fireComponentDetachEvent(window);
//        fireWindowOrder(Collections.singletonMap(-1, window));
        return true;
    }
    
    /**
     * Gets all the windows added to this UI.
     *
     * @return an unmodifiable collection of windows
     */
    public Collection<SubWindow> getWindows() {
        return windowDesktopArea.getWindows();
    }

    @Override
    protected SubWindowDesktopState getState() {
        return (SubWindowDesktopState)super.getState();
        
    }
    
    @Override
    protected SubWindowDesktopState getState(boolean markAsDirty) {
        return (SubWindowDesktopState) super.getState(markAsDirty);
    }

    @Override
    public void windowClose(SubWindow.CloseEvent e) {
        for (Iterator<Component> iterator = openedWindowsBar.iterator(); iterator.hasNext();) {
            Button next = (Button)iterator.next();
            if (next.getData() == e.getWindow()) {
                openedWindowsBar.removeComponent(next);
            }
        }
    }
    
}