/*
 * This file is part of the Illarion project.
 *
 * Copyright © 2016 - Illarion e.V.
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
package illarion.client.util.account;

import com.google.common.util.concurrent.ListenableFuture;
import illarion.client.IllaClient;
import illarion.client.Servers;
import illarion.client.util.account.form.AccountCheckForm;
import illarion.client.util.account.form.AccountCreateForm;
import illarion.client.util.account.form.CharacterCreateForm;
import illarion.client.util.account.request.*;
import illarion.client.util.account.response.*;
import illarion.common.types.CharacterId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.illarion.engine.Option;
import org.illarion.engine.ui.login.LoginData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class AccountSystem implements AutoCloseable {
    private final static Logger LOGGER = LogManager.getLogger();

    public final static String OFFICIAL_ENDPOINT = "https://illarion.org/app.php";

    private final Object requestHandlerLock = new Object();

    @Nullable
    private String endpoint;

    @Nullable
    private IllarionAuthenticator authenticator;

    @Nullable
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private RequestHandler requestHandler;

    private Servers currentServer;

    public void setupAuthentication(LoginData loginData) {
        var usedServer = Arrays.stream(Servers.values())
                .filter(server -> server.getServerName().equals(loginData.server()))
                .findFirst()
                .orElse(Servers.Illarionserver);

        if (usedServer == Servers.Localserver ||
                (usedServer == Servers.Customserver
                        && !IllaClient.getConfig().getBoolean(Option.customServerAccountSystem))) {
            LOGGER.debug("AccountSystem not active, executing a direct login");
            return;
        }

        var serverEndpoint = usedServer == Servers.Customserver
                ? "https://" + usedServer.getServerHost() + "/app.php"
                : AccountSystem.OFFICIAL_ENDPOINT;

        currentServer = usedServer;
        authenticator = new IllarionAuthenticator(loginData.username(), loginData.password());
        endpoint = serverEndpoint;
    }

    public String getLoginPassword() {
        if (authenticator == null) return "";
        return new String(authenticator.getPasswordAuthentication().getPassword());
    }

    public Servers getCurrentServer() {
        return currentServer;
    }


    public void setEndpoint(String customEndpoint) {
        if (customEndpoint.equals(endpoint)) {
            return;
        }

        try {
            closeRequestHandler();
        } catch (Exception e) {
            LOGGER.debug("Failed to close AccountSystem RequestHandler");
        }

        endpoint = customEndpoint;
    }

    @NotNull
    private IllarionAuthenticator getAuthenticator() {
        IllarionAuthenticator authenticator = this.authenticator;
        if (authenticator == null) {
            throw new IllegalStateException("The request requires authentication credentials.");
        }
        return authenticator;
    }

    @NotNull
    public ListenableFuture<AccountGetResponse> getAccountInformation() {
        RequestHandler handler = getRequestHandler();
        IllarionAuthenticator authenticator = getAuthenticator();

        Request<AccountGetResponse> request = new AccountGetRequest(authenticator);

        return handler.sendRequestAsync(request);
    }

    @NotNull
    public ListenableFuture<CharacterGetResponse> getCharacterInformation(@NotNull String serverId,
                                                                          @NotNull CharacterId id) {
        RequestHandler handler = getRequestHandler();
        IllarionAuthenticator authenticator = getAuthenticator();

        Request<CharacterGetResponse> request = new CharacterGetRequest(authenticator, serverId, id);

        return handler.sendRequestAsync(request);
    }

    @NotNull
    public ListenableFuture<AccountCheckResponse> performAccountCredentialsCheck(@Nullable String username,
                                                                                 @Nullable String email) {
        RequestHandler handler = getRequestHandler();

        AccountCheckForm payload = new AccountCheckForm(username, email);
        Request<AccountCheckResponse> request = new AccountCheckRequest(payload);

        return handler.sendRequestAsync(request);
    }

    @NotNull
    public ListenableFuture<AccountCreateResponse> createAccount(@NotNull String username, @NotNull String password) {
        var accountCreationForm = new AccountCreateForm(username, password, null);
        var accountCreationRequest = new AccountCreateRequest(accountCreationForm);

        return getRequestHandler().sendRequestAsync(accountCreationRequest);
    }

    @NotNull
    public ListenableFuture<AccountCreateResponse> createAccount(@NotNull String username,
                                                                 @Nullable String email,
                                                                 @NotNull String password) {
        RequestHandler handler = getRequestHandler();

        AccountCreateForm payload = new AccountCreateForm(username, password, email);
        Request<AccountCreateResponse> request = new AccountCreateRequest(payload);

        return handler.sendRequestAsync(request);
    }

    @NotNull
    public ListenableFuture<CharacterCreateGetResponse> getPossibleCharacterCreationSpecifications() {
        RequestHandler handler = getRequestHandler();
        IllarionAuthenticator authenticator = getAuthenticator();

        Request<CharacterCreateGetResponse> request = new CharacterCreateGetRequest(authenticator, currentServer.getServerName());

        return handler.sendRequestAsync(request);
    }

    @NotNull
    public ListenableFuture<CharacterCreateResponse> createCharacter(@NotNull CharacterCreateForm characterCreateForm) {
        var requestHandler = getRequestHandler();
        var authenticator = getAuthenticator();

        var request = new CharacterCreateRequest(authenticator, currentServer.getServerName(), characterCreateForm);

        return requestHandler.sendRequestAsync(request);
    }

    @Override
    public void close() throws Exception {
        closeRequestHandler();
    }

    @NotNull
    private RequestHandler getRequestHandler() {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalStateException("Communicating with the account system is not possible until the endpoint is set.");
        }

        if (requestHandler != null) {
            return requestHandler;
        }

        synchronized (requestHandlerLock) {
            if (requestHandler == null) {
                requestHandler = new RequestHandler(endpoint);
            }
        }

        return requestHandler;
    }

    private void closeRequestHandler() throws Exception {
        RequestHandler handler = requestHandler;
        requestHandler = null;
        if (handler != null) {
            handler.close();
        }
    }
}
