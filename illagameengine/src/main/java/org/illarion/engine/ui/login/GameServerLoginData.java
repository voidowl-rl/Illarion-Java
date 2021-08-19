package org.illarion.engine.ui.login;

import illarion.common.types.CharacterId;

public record GameServerLoginData(CharacterId characterId, String accountPassword) {
}
