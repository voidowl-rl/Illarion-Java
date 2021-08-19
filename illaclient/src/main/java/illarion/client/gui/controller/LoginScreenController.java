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
package illarion.client.gui.controller;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import illarion.client.IllaClient;
import illarion.client.Servers;
import illarion.client.gui.login.CharacterCreationOptionsConverter;
import illarion.client.resources.SongFactory;
import illarion.client.util.AudioPlayer;
import illarion.client.util.Lang;
import illarion.client.util.account.AccountSystem;
import illarion.client.util.account.response.AccountCreateResponse;
import illarion.client.util.account.services.LoginService;
import illarion.common.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.illarion.engine.*;
import org.illarion.engine.assets.Assets;
import org.illarion.engine.backend.gdx.events.ResolutionChangedEvent;
import org.illarion.engine.sound.Music;
import org.illarion.engine.sound.Sounds;
import org.illarion.engine.ui.CharacterCreation;
import org.illarion.engine.ui.UserInterface;
import org.illarion.engine.ui.login.*;
import org.illarion.engine.ui.stage.LoginStage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static illarion.client.gui.login.CharacterCreationOptionsConverter.convertToCharacterCreateForm;

/**
 * This is the screen controller that takes care of displaying the login screen.
 */
public final class LoginScreenController implements ScreenController {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<String> RESOLUTION_OPTIONS = Arrays.asList(Option.fullscreen, Option.fullscreenHeight,
            Option.fullscreenWidth, Option.fullscreenWidth, Option.fullscreenBitsPerPoint, Option.fullscreenRefreshRate,
            Option.deviceName, Option.windowHeight, Option.windowWidth);

    private final UserInterface gui;
    private final Sounds sounds;
    private final Assets assets;
    private final Window window;
    private final AccountSystem accountSystem;
    private final ExecutorService executor;
    private final LoginService loginService;

    private LoginStage stage;

    public LoginScreenController(BackendBinding binding, AccountSystem accountSystem) {
        this.gui = binding.getGui();
        this.sounds = binding.getSounds();
        this.assets = binding.getAssets();
        this.window = binding.getWindow();
        this.accountSystem = accountSystem;
        this.executor = Executors.newCachedThreadPool();
        this.loginService = new illarion.client.util.account.services.LoginService(accountSystem, executor);
    }

    @Override
    public void onStartScreen() {
        AudioPlayer audioPlayer = AudioPlayer.INSTANCE;
        audioPlayer.initAudioPlayer(sounds);
        Music illarionTheme = SongFactory.getInstance().getSong(2, assets.getSoundsManager());
        audioPlayer.setLastMusic(illarionTheme);
        if (IllaClient.getConfig().getBoolean(Option.musicOn)
                && illarionTheme != null
                && !audioPlayer.isCurrentMusic(illarionTheme)) { // may be null in case OpenAL is not working
            audioPlayer.playMusic(illarionTheme);
        }

        int serverKey = IllaClient.DEFAULT_SERVER.getServerKey();

        var servers = Servers.values();
        var loginData = new LoginData[servers.length];
        for (var server : servers) {
            var isCustomServer = server == Servers.Customserver;
            loginData[server.getServerKey()] = new LoginData(
                    server.getServerKey(),
                    server.getServerName(),
                    isCustomServer
                            ? IllaClient.getConfig().getString("customLastLogin")
                            : IllaClient.getConfig().getString("lastLogin"),
                    isCustomServer
                            ? IllaClient.getConfig().getString("customFingerprint")
                            : IllaClient.getConfig().getString("fingerprint"),
                    isCustomServer
                            ? IllaClient.getConfig().getBoolean("customSavePassword")
                            : IllaClient.getConfig().getBoolean("savePassword"));
        }

        stage = gui.activateLoginStage(Lang.INSTANCE.getLoginResourceBundle());
        stage.setLoginData(loginData, serverKey);
        stage.setExitListener(IllaClient::exit);
        stage.setLoginListener(this::onLoginIssued);
        stage.setOptionsData(IllaClient.getConfig(), window.getResolutionManager());
        stage.setOptionsSaveListener(LoginScreenController::saveOptions);
        stage.setCharacterCreationListener(this::onCharacterCreationIssued);
        stage.setAccountCreationListener(this::onAccountCreationIssued);
    }

    @Override
    public void onEndScreen() {}

    public void setLoginListener(Consumer<GameServerLoginData> loginListener) {
        stage.setCharacterSelectionListener((characterId) ->
                loginListener.accept(new GameServerLoginData(characterId, accountSystem.getLoginPassword())));
    }

    private void onAccountCreationIssued(AccountCreationData accountCreationData) {
        accountSystem.setEndpoint(AccountSystem.OFFICIAL_ENDPOINT);

        var accountCreationRequest = Futures.transformAsync(
                accountSystem.performAccountCredentialsCheck(accountCreationData.name(), accountCreationData.email()),
                (accountCheckResponse) -> {
                    if (accountCheckResponse == null || accountCheckResponse.getError() != null) {
                        stage.accountCreationFailed();
                        throw new RuntimeException("Account creation failed");
                    }

                    for (var check : accountCheckResponse.getChecks()) {
                        if (!check.isSuccess()) {
                            throw new RuntimeException("Account creation failed");
                        }
                    }

                    return accountSystem.createAccount(
                            accountCreationData.name(),
                            //accountCreationData.email(), TODO: Server currently does not support account creation with mail (request throws 500)
                            accountCreationData.password());
                },
                executor);

        Futures.addCallback(
                accountCreationRequest,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable AccountCreateResponse result) {
                        stage.accountCreationSuccessful();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        stage.accountCreationFailed();
                    }
                },
                executor);
    }

    private void onLoginIssued(LoginData loginData) {
        accountSystem.setupAuthentication(loginData);

        var loginRequests = Futures.allAsList(
                loginService.getAccountCharacterList(),
                Futures.transform(
                        accountSystem.getPossibleCharacterCreationSpecifications(),
                        CharacterCreationOptionsConverter::convertToCharacterCreationOptions,
                        executor));

        Futures.addCallback(loginRequests, new LoginFinishedCallback(), executor);
    }

    private void onCharacterCreationIssued(CharacterCreation characterData) {
        var characterCreationRequest = accountSystem.createCharacter(convertToCharacterCreateForm(characterData));
        var updateCharacterListRequest = Futures.transformAsync(
                characterCreationRequest,
                (ignore) -> loginService.getAccountCharacterList(),
                executor);

        Futures.addCallback(
                updateCharacterListRequest,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable CharacterSelectionData[] result) {
                        stage.loginSuccessful(result);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        stage.characterCreationFailed();
                    }
                },
                executor);
    }

    private static void saveOptions(Map<String, String> options) {
        var config = IllaClient.getConfig();

        var changedOptions = options.entrySet().stream()
                .filter(option -> isOptionChanged(option.getKey(), option.getValue(), config))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        changedOptions.forEach(config::set);
        config.save();

        // Special handling for options (applied as one event not as several)
        if (changedOptions.keySet().stream().anyMatch(RESOLUTION_OPTIONS::contains)) {
            EventBus.INSTANCE.post(new ResolutionChangedEvent(config));
        }
    }

    private static boolean isOptionChanged(String option, String optionValue, ConfigReader config) {
        var configOptionValue = config.getString(option);
        return !optionValue.equals(configOptionValue);
    }

    private class LoginFinishedCallback implements FutureCallback<List<Object>> {
        @Override
        public void onSuccess(@Nullable List<Object> result) {
            if (result == null || result.size() != 2) {
                throw new RuntimeException("Something went wrong.");
            }

            CharacterSelectionData[] characterList;
            CharacterCreationOptions characterCreationOptions;

            try {
                characterList = (CharacterSelectionData[]) result.get(0);
                characterCreationOptions = (CharacterCreationOptions) result.get(1);
            } catch (ClassCastException e) {
                throw new RuntimeException("Something went wrong.");
            }

            stage.setCharacterCreationOptions(characterCreationOptions);
            stage.loginSuccessful(characterList);
        }

        @Override
        public void onFailure(Throwable t) {
            LOGGER.warn(t.getMessage());
            stage.loginFailed();
        }
    }
}
