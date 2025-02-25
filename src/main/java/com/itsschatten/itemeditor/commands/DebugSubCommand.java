package com.itsschatten.itemeditor.commands;

import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * A little command used to debug certain aspects of the plugin during development.
 * Usually the command is does nothing.
 */
public final class DebugSubCommand extends BrigadierCommand {

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("debug")
                .executes(context -> {
                    // This will ALWAYS return 1 as this command is built to always do something.
                    return 1;
                });
    }
}
