/*
 * This file is part of the Illarion project.
 *
 * Copyright © 2015 - Illarion e.V.
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

import illarion.client.gui.controller.LoginScreenController;
import illarion.client.gui.controller.ScreenController;
import illarion.client.util.account.AccountSystem;
import org.illarion.engine.BackendBinding;
import org.illarion.engine.State;

import java.util.function.Consumer;

/**
 * This game state is used to display the login and character selection dialog. Also the option dialog is displayed in
 * this state.
 */
public class LoginState implements GameState {
    /**
     * The screen controller that takes care for the login screen.
     */
    private LoginScreenController loginController;

    @Override
    public void create(BackendBinding binding) {
        loginController = new LoginScreenController(binding, new AccountSystem());
    }

    @Override
    public void dispose() {
    }

    @Override
    public void update(int delta) {
    }

    @Override
    public void render() {
    }

    @Override
    public boolean isClosingGame() {
        return true;
    }

    @Override
    public void enterState(Consumer<State> enterNextState) {
        /*loginController.setLoginListener((loginData) -> {
            enterNextState.accept(State.PLAYING);
            World.getNet().sendCommand(new LoginCmd(loginData.character, loginData.password, IllaClient.APPLICATION.getApplicationVersion()));
        });*/

        loginController.onStartScreen();
    }

    @Override
    public void leaveState() {
    }
}
