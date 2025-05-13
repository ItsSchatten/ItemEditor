package com.itsschatten.itemeditor.utils;

import com.itsschatten.utilities.UpdateResolversEvent;
import com.itsschatten.yggdrasil.StringUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public final class ResolverListener implements Listener {

    @EventHandler
    public void onUpdateResolvers(final @NotNull UpdateResolversEvent event) {
        StringUtil.addResolvers(event.getResolver());
    }

}
