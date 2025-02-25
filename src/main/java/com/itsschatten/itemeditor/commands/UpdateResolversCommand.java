package com.itsschatten.itemeditor.commands;

import com.itsschatten.itemeditor.ItemEditorPlugin;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public final class UpdateResolversCommand extends BrigadierCommand {

    public UpdateResolversCommand() {
        super("Reload all custom tag resolvers.");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("updateresolvers")
                .executes(context -> {
                    StringUtil.clearResolvers();
                    ItemEditorPlugin.configureResolvers(ItemEditorPlugin.getProvidingPlugin(ItemEditorPlugin.class), context.getSource().getSender());
                    return 1;
                });
    }

}
