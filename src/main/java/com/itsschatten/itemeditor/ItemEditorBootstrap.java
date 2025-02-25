package com.itsschatten.itemeditor;

import com.itsschatten.itemeditor.commands.ItemEditorCommand;
import com.itsschatten.itemeditor.commands.ListResolversCommand;
import com.itsschatten.itemeditor.commands.UpdateResolversCommand;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class ItemEditorBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        final LifecycleEventManager<@NotNull BootstrapContext> manager = context.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();

            new ItemEditorCommand().register(registrar);
            new UpdateResolversCommand().register(registrar);
            new ListResolversCommand().register(registrar);
        });
    }

}
