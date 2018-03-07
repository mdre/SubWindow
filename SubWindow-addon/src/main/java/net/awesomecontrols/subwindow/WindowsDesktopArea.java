/*
 * Copyright 2018 Marcelo D. Ré {@literal <marcelo.re@gmail.com>}.
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

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Panel;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class WindowsDesktopArea extends Panel {
    private final static Logger LOGGER = Logger.getLogger(WindowsDesktopArea.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    /**
     * List of windows in this UI.
     */
    private final LinkedHashSet<SubWindow> windows = new LinkedHashSet<>();
    private WindowsDesktopLayout windowsDesktopLayout = new WindowsDesktopLayout();
    public WindowsDesktopArea() {
        windowsDesktopLayout.addStyleName("windowsDesktopArea");
        this.setContent(this.windowsDesktopLayout);
    }
    
    
    /**
     * Add a subwindow to the desktop.
     *
     * @param sw
     *            the subwindow to be added
     * @return this
     */
    public WindowsDesktopArea addSubWindow(SubWindow sw) {
        this.windows.add(sw);
        this.windowsDesktopLayout.addComponent(sw);
        return this;
    }
    
        /**
     * Remove the window from the desktop
     *
     * @param window
     *            the window to be removed
     * @return true if success
     */
    public boolean removeWindow(SubWindow window) {
        if (!windows.remove(window)) {
            // Window window is not a subwindow of this UI.
            return false;
        }
        window.setParent(null);
        markAsDirty();
        window.fireClose();
        this.windowsDesktopLayout.removeComponent(window);
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
        return windows;
//        return Collections.unmodifiableCollection(windows);
    }
}
