/*
 * This file is part of the Illarion project.
 *
 * Copyright © 2014 - Illarion e.V.
 *
 * Illarion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Illarion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package illarion.client.state;

import org.illarion.engine.BackendBinding;
import org.illarion.engine.State;

import java.util.function.Consumer;

/**
 * This interface defines the different states the game is able to enter.
 */
public interface GameState {
    /**
     * This function is called once the game is created.
     * <p/>
     * Its called for all game states, not just for the active one.
     */
    void create(BackendBinding binding);

    /**
     * This function is called once the game is shut down.
     * <p/>
     * Its called for all game states, not just for the active one.
     */
    void dispose();

    /**
     * This function is called during the update loop for the active game state.
     *
     * @param delta the time since the last update in milliseconds
     */
    void update(int delta);

    /**
     * This function is called during the render loop for the active game state.
     */
    void render();

    /**
     * This function is called once the game is requested to close. Its called for the active game state. This state
     * is allowed to choose if the game is supposed to quit or interrupt the exit progress.
     *
     * @return {@code true} if its okay that the game is closing
     */
    boolean isClosingGame();

    /**
     * This function is called once the game is entered
     */
    void enterState(Consumer<State> enterNextState);

    /**
     * This function is called once a formerly active state is left.
     */
    void leaveState();
}
