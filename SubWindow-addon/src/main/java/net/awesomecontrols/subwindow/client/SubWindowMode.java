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

package net.awesomecontrols.subwindow.client;

/**
 *
 * @author SShadow
 */
public enum SubWindowMode {
    /**
     * Normal mode. The window size and position is determined by the window
     * state.
     */
    NORMAL,
    /**
     * Maximized mode. The window is positioned in the top left corner and fills
     * the whole screen.
     */
    MAXIMIZED,
    
    /**
     *
     */
    MINIMIZED,
    
    /**
     *
     */
    HIDDEN;
}
