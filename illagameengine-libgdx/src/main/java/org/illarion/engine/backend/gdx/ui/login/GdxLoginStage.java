package org.illarion.engine.backend.gdx.ui.login;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import illarion.common.config.ConfigReader;
import illarion.common.types.CharacterId;
import org.illarion.engine.backend.gdx.GdxRenderable;
import org.illarion.engine.graphic.ResolutionManager;
import org.illarion.engine.ui.*;
import org.illarion.engine.ui.login.AccountCreationData;
import org.illarion.engine.ui.login.CharacterCreationOptions;
import org.illarion.engine.ui.login.CharacterSelectionData;
import org.illarion.engine.ui.login.LoginData;
import org.illarion.engine.ui.stage.LoginStage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public class GdxLoginStage implements LoginStage, GdxRenderable {
    private final Stage stage;
    private final Container<Table> root;

    private final CreditView credits;
    private final OptionsView options;
    private final LoginView login;
    private final CharacterSelectView characterSelection;
    private final CharacterCreationView characterCreation;
    private final AccountCreationView accountCreation;

    private final Dialog dialog;
    private final TextButton btDialog;
    private final NullSecureResourceBundle resourceBundle;

    private Action dialogConfirmationCallback;

    private Table activeTable;

    public GdxLoginStage(Skin skin, NullSecureResourceBundle resourceBundle) {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        root = new Container<>();
        root.setFillParent(true);
        //root.setBackground(getBackgroundDrawable());
        root.center();
        root.fill();
        stage.addActor(root);

        this.resourceBundle = resourceBundle;
        btDialog = new TextButton("OK", skin);
        btDialog.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
                if (dialogConfirmationCallback != null) {
                    dialogConfirmationCallback.invoke();
                }
            }
        });
        dialog = new Dialog("Attention", skin, "dialog");

        login = new LoginView(skin, resourceBundle);
        credits = new CreditView(skin, resourceBundle);
        options = new OptionsView(skin, resourceBundle);
        characterSelection = new CharacterSelectView(skin, resourceBundle);
        characterCreation = new CharacterCreationView(skin, resourceBundle);
        accountCreation = new AccountCreationView(skin, resourceBundle);

        login.setOnOptionsCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activateTable(options);
            }
        });
        login.setOnCreditsCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activateTable(credits);
            }
        });
        login.setOnCreateAccountCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activateTable(accountCreation);
            }
        });

        characterSelection.setOnLogoutCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activateTable(login);
            }
        });
        characterSelection.setOnCreateCharacterCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activateTable(characterCreation);
            }
        });

        characterCreation.setOnCancelCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activateTable(characterSelection);
            }
        });

        options.setOnBackCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activateTable(login);
            }
        });

        accountCreation.setOnBackCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activateTable(login);
            }
        });

        activateTable(login);
    }

    @Override
    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();

        if (activeTable == credits && credits.cycleCredits()) {
            activateTable(login);
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        root.setSize(width, height);
    }

    public void dispose() {
        stage.dispose();
    }

    @Override
    public void setLoginData(LoginData[] data, int initialServer) {
        login.setLoginData(data, initialServer);
    }

    @Override
    // Will run in main thread
    public void loginSuccessful(CharacterSelectionData[] data) {
        Gdx.app.postRunnable(() -> {
            characterSelection.setCharacters(data);
            dialog.hide();
            activateTable(characterSelection);
        });
    }

    @Override
    // Will run in main thread
    public void loginFailed() {
        Gdx.app.postRunnable(() -> showDialog("error", null));
    }

    @Override
    public void accountCreationFailed() {
        Gdx.app.postRunnable(() -> showDialog("error", null));
    }

    @Override
    public void accountCreationSuccessful() {
        Gdx.app.postRunnable(() -> showDialog("success", () -> activateTable(login)));
    }

    @Override
    public void characterCreationFailed() {
        Gdx.app.postRunnable(() -> showDialog("error", null));
    }

    @Override
    public void setOptionsData(ConfigReader configReader, ResolutionManager resolutionManager) {
        options.setOptions(configReader, resolutionManager);
    }

    @Override
    public void setCharacterCreationOptions(CharacterCreationOptions characterCreationOptions) {
        characterCreation.setOptions(characterCreationOptions);
    }

    @Override
    public void setExitListener(Action event) {
        login.setOnExitCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
                event.invoke();
            }
        });
    }

    @Override
    public void setLoginListener(Consumer<LoginData> event) {
        login.setOnLoginCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
                showDialog("login");
                event.accept(login.getSelectedLoginData());
            }
        });
    }

    @Override
    public void setOptionsSaveListener(Consumer<Map<String, String>> event){
        options.setOnSaveCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
                event.accept(options.getCurrentSettings());
            }
        });
    }

    @Override
    public void setCharacterSelectionListener(Consumer<CharacterId> event) {
        characterSelection.setOnPlayCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
                characterSelection.getSelectedCharacter().ifPresent((character) -> event.accept(character.id));
            }
        });
    }

    @Override
    public void setCharacterCreationListener(Consumer<CharacterCreation> event) {
        characterCreation.setOnFinishCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
                event.accept(characterCreation.getCharacter());
            }
        });
    }

    @Override
    public void setAccountCreationListener(Consumer<AccountCreationData> event) {
        accountCreation.setOnCreateAccountCallback(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
                showDialog("creation");

                if (!accountCreation.isPasswordMatching()) {
                    showDialog("creationFailedPassword");
                }

                if (!accountCreation.isEmailMatching()) {
                    showDialog("creationFailedEmail");
                }

                event.accept(accountCreation.getAccountCreationData());
            }
        });
    }

    private void activateTable(Table table) {
        if (activeTable != null) {
            activeTable.remove();
        }

        activeTable = table;
        root.setActor(activeTable);
    }

    private void showDialog(@NotNull String dialogTextLocalizationKey) {
        dialog.getContentTable().clearChildren();
        dialog.getButtonTable().clearChildren();

        dialog.text(resourceBundle.getLocalizedString(dialogTextLocalizationKey));

        dialog.pack();
        dialog.show(stage);
    }

    private void showDialog(@NotNull String dialogTextLocalizationKey, @Nullable Action confirmationCallback) {
        dialog.getContentTable().clearChildren();

        dialogConfirmationCallback = confirmationCallback;
        dialog.button(btDialog);

        dialog.text(resourceBundle.getLocalizedString(dialogTextLocalizationKey));

        dialog.pack();
        dialog.show(stage);
    }
}
