package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseRemainder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public final class ConvertSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie convert <secondary><item></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Set the item your item will convert to when used.
                        <gray>This uses the normal Minecraft item.</gray>
                        \s
                        ◼ <secondary><item><required></secondary> The item to convert to.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie convert "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("convert")
                .then(literal("-reset")
                        .executes(this::removeRemainder)
                )
                .then(argument("item", ArgumentTypes.itemStack())
                        .executes(context -> updateUseRemainder(context, () -> {
                            final ItemStack item = context.getArgument("item", ItemStack.class);
                            final ItemMeta meta = item.getItemMeta();

                            final Component heldItemName = (Objects.requireNonNull(meta.hasDisplayName() ? meta.displayName() : meta.hasItemName() ? meta.itemName() : Component.translatable(item)))
                                    .hoverEvent(item.asHoverEvent()).colorIfAbsent(meta.hasRarity() ? meta.getRarity().color() : NamedTextColor.WHITE);
                            final Component component = StringUtil.color("<primary>Updated your item's use remainder to ").append(StringUtil.color("<dark_gray>[</dark_gray>").append(heldItemName).append(StringUtil.color("<dark_gray>]</dark_gray>"))).append(StringUtil.color("<primary>!"));

                            Utils.tell(context.getSource(), component);
                            return item;
                        }))
                );
    }

    /**
     * Updates the {@link DataComponentTypes#USE_REMAINDER} on the command senders held {@link ItemStack}.
     *
     * @param context  The {@link CommandContext} used to get the sender and the item they are holding.
     * @param function A {@link Supplier} that provides the {@link ItemStack} for use as the {@link DataComponentTypes#USE_REMAINDER}.
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int updateUseRemainder(final @NotNull CommandContext<CommandSourceStack> context, final Supplier<ItemStack> function) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        stack.setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(function.get()));
        return 1;
    }

    /**
     * Removes the {@link DataComponentTypes#USE_REMAINDER} component from the senders {@link ItemStack}.
     *
     * @param context The {@link CommandContext} used to get the sender and the item they are holding.
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int removeRemainder(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        stack.unsetData(DataComponentTypes.USE_REMAINDER);
        Utils.tell(context, "<primary>Removed the use remainder from your item!");
        return 1;
    }

}
