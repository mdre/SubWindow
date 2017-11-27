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

package net.awesomecontrols.subwindow.client;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.event.shared.GwtEvent;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class SubWindowMoveEvent  extends GwtEvent<SubWindowMoveHandler> {
    private final static Logger LOGGER = Logger.getLogger(SubWindowMoveEvent.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }

    private static final Type<SubWindowMoveHandler> TYPE = new Type<>();

    private final int newX;
    private final int newY;

    /**
     * Creates a new event with the given parameters
     *
     * @param x
     *            The new x-position for the VWindow
     * @param y
     *            The new y-position for the VWindow
     */
    public SubWindowMoveEvent(int x, int y) {
        newX = x;
        newY = y;
    }

    /**
     * Gets the new x position of the window
     *
     * @return the new X position of the VWindow
     */
    public int getNewX() {
        return newX;
    }

    /**
     * Gets the new y position of the window
     *
     * @return the new Y position of the VWindow
     */
    public int getNewY() {
        return newY;
    }
    
    
     /**
     * Gets the type of the event
     *
     * @return the type of the event
     */
    public static Type<SubWindowMoveHandler> getType() {
        return TYPE;
    }

    @Override
    public Type<SubWindowMoveHandler> getAssociatedType() {
        return TYPE;
    }
    
    
    @Override
    protected void dispatch(SubWindowMoveHandler handler) {
        handler.onWindowMove(this);
    }
}

    
    
