package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public final class RenameSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie rename <secondary><name></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the item's default name.
                \s
                ◼ <secondary><name><required> </secondary> The name for the item.
                ◼ <secondary>[-view]<optional></secondary> View the item's current name.
                ◼ <secondary>[-clear]<optional></secondary> Clear the name.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie name "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("rename").redirect(new ItemNameSubCommand().command().build());
    }

}
