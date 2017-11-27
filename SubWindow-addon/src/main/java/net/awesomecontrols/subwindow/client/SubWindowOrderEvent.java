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

import com.google.gwt.event.shared.GwtEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class SubWindowOrderEvent  extends GwtEvent<SubWindowOrderHandler> {
    private final static Logger LOGGER = Logger.getLogger(SubWindowOrderEvent.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }

    private static final Type<SubWindowOrderHandler> TYPE = new Type<>();

    private final List<SubWindowWidget> windows;

    /**
     * Creates a new event with the given order.
     *
     * @param windows
     *            The new order position for the SubWindowWidget
     */
    public SubWindowOrderEvent(List<SubWindowWidget> windows) {
        this.windows = windows;
    }

    @Override
    public Type<SubWindowOrderHandler> getAssociatedType() {
        return TYPE;
    }

    /**
     * Returns windows in order.
     *
     * @return windows in the specific order
     */
    public SubWindowWidget[] getWindows() {
        return windows.toArray(new SubWindowWidget[windows.size()]);
    }

    @Override
    protected void dispatch(SubWindowOrderHandler handler) {
        handler.onWindowOrderChange(this);
    }

    /**
     * Gets the type of the event.
     *
     * @return the type of the event
     */
    public static Type<SubWindowOrderHandler> getType() {
        return TYPE;
    }

}


