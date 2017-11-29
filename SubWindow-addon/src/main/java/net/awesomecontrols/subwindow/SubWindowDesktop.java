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

import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class SubWindowDesktop extends Panel {
    private final static Logger LOGGER = Logger.getLogger(SubWindowDesktop.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    List<SubWindow> subwindows = new ArrayList<>();

    private CssLayout layout = new CssLayout();
    
    public SubWindowDesktop() {
        this.init();
    }

    public SubWindowDesktop(Component content) {
        super(content);
        this.init();
    }

    public SubWindowDesktop(String caption) {
        super(caption);
        this.init();
    }

    public SubWindowDesktop(String caption, Component content) {
        super(caption, content);
        this.init();
    }
    
    private void init() {
        layout.setSizeFull();
        this.setContent(layout);
    }
    
    public SubWindowDesktop addSubWindow(SubWindow sw) {
        this.subwindows.add(sw);
        this.layout.addComponent(sw);
        return this;
    }
    
    public List<SubWindow> getWindows() {
        return subwindows;
    }
}
