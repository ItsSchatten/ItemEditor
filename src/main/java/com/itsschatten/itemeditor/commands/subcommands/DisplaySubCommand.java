package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
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

public class DisplaySubCommand extends BrigadierCommand {
    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie display <secondary><name></secondary>").hoverEvent(StringUtil.color("""
                <primary>Renames the item.
                \s
                ◼ <secondary><name></secondary><required> The name for the item.
                ◼ <secondary>[-view]<optional></secondary> View the item's current display name.
                ◼ <secondary>[-clear]<optional></secondary> Clear the name.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie display "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("display")
                .then(Commands.argument("display name", StringArgumentType.greedyString())
                        .executes(context -> updateDisplay(context, (stack, meta) -> {
                            final Component component = StringUtil.color("<!i>" + StringArgumentType.getString(context, "display name"));
                            meta.displayName(component);

                            Utils.tell(context.getSource(), StringUtil.color("<primary>Set your item's display name to <reset>'")
                                    .append(component)
                                    .append(StringUtil.color("<reset>'<primary>.")));
                            return meta;
                        }))
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of(getName(context)), builder))
                )
                .then(Commands.literal("-clear")
                        .executes(context -> updateDisplay(context, (stack, meta) -> {
                            meta.displayName(null);

                            Utils.tell(context.getSource(), StringUtil.color("<primary>Your item's display name has been reset to <white>" +
                                    (meta.hasItemName() ? MiniMessage.miniMessage().serialize(meta.itemName()) + " <gray>(<i>custom item name</i>)</gray>"
                                            : stack.getType().getKey().getKey().toLowerCase().replace("_", " ")) + "</white>."));
                            return meta;
                        }))
                )
                .then(Commands.literal("-view")
                        .executes(this::handleView)
                )
                .executes(context -> updateDisplay(context, (stack, meta) -> {
                    meta.displayName(Component.text(""));
                    Utils.tell(context.getSource(), StringUtil.color("<primary>Your item's display name has been set to <secondary>an empty string</secondary>."));
                    return meta;
                }));
    }

    private @NotNull String getName(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            return "";
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return "";
        }

        return meta.hasDisplayName() ? MiniMessage.miniMessage().serialize(Objects.requireNonNull(meta.displayName())) : "";
    }

    private int updateDisplay(final @NotNull CommandContext<CommandSourceStack> context, BiFunction<ItemStack, ItemMeta, ItemMeta> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        stack.setItemMeta(function.apply(stack, meta));
        return 1;
    }

    private int handleView(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        if (meta.hasDisplayName()) {
            Utils.tell(user, StringUtil.color("<primary>Your item's display name is currently:").appendSpace().append(Objects.requireNonNull(meta.displayName())).colorIfAbsent(NamedTextColor.WHITE).append(StringUtil.color("<primary>.")));
        } else {
            Utils.tell(user, "<primary>Your item doesn't currently have a display item name.");
        }
        return 1;
    }
}
