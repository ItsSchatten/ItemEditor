package com.itsschatten.itemeditor.commands;

import com.itsschatten.itemeditor.ItemEditorPlugin;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public final class ListResolversCommand extends BrigadierCommand {

    public ListResolversCommand() {
        super("List all of the resolvers loaded for this plugin.");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("listresolvers")
                .then(argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            Utils.tell(context.getSource(), ItemEditorPlugin.listResolvers(ItemEditorPlugin.getProvidingPlugin(ItemEditorPlugin.class), IntegerArgumentType.getInteger(context, "page")));
                            return 1;
                        })
                )
                .executes(context -> {
                    Utils.tell(context.getSource(), ItemEditorPlugin.listResolvers(ItemEditorPlugin.getProvidingPlugin(ItemEditorPlugin.class), 1));
                    return 1;
                });
    }
}
