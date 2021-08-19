package org.illarion.engine.ui.stage;

import illarion.common.config.ConfigReader;
import illarion.common.types.CharacterId;
import org.illarion.engine.graphic.ResolutionManager;
import org.illarion.engine.ui.Action;
import org.illarion.engine.ui.CharacterCreation;
import org.illarion.engine.ui.login.AccountCreationData;
import org.illarion.engine.ui.login.CharacterCreationOptions;
import org.illarion.engine.ui.login.CharacterSelectionData;
import org.illarion.engine.ui.login.LoginData;

import java.util.Map;
import java.util.function.Consumer;

public interface LoginStage {
    void setLoginData(LoginData[] data, int initialServer);
    void loginSuccessful(CharacterSelectionData[] data);
    void loginFailed();

    void accountCreationFailed();

    void accountCreationSuccessful();

    void setOptionsData(ConfigReader configReader, ResolutionManager resolutionManager);

    void setExitListener(Action event);
    void setLoginListener(Consumer<LoginData> event);
    void setOptionsSaveListener(Consumer<Map<String, String>> event);
    void setCharacterSelectionListener(Consumer<CharacterId> characterId);
    void setCharacterCreationListener(Consumer<CharacterCreation> data);

    void setAccountCreationListener(Consumer<AccountCreationData> event);


    void setCharacterCreationOptions(CharacterCreationOptions characterCreationOptions);

    void characterCreationFailed();
}
