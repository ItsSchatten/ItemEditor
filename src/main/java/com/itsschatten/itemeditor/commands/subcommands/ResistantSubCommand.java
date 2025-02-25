package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.RegistryArgumentExtractor;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DamageResistant;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class ResistantSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie resistance <secondary><type></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set whether your item is resistant to a type of damage or not.
                \s
                ◼ <secondary><type><required> [true|false]<optional></secondary> Set the damage resistance for your item.
                ◼ <secondary>[-view]<optional></secondary> View the item's current resistance status.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie resistance "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("resistant")
                .then(argument("type", ArgumentTypes.resourceKey(RegistryKey.DAMAGE_TYPE))
                        .executes(context -> updateResistance(context, () -> DamageResistant.damageResistant(TagKey.create(RegistryKey.DAMAGE_TYPE, RegistryArgumentExtractor.getTypedKey(context, RegistryKey.DAMAGE_TYPE, "type").key()))))
                )
                .then(literal("-reset")
                        .executes(this::reset)
                )
                .then(literal("-view")
                        .executes(this::handleView)
                );
    }

    private int reset(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.resetData(DataComponentTypes.DAMAGE_RESISTANT);
        Utils.tell(context, "<primary>Your item's damage resistance has been reset.");
        return 1;
    }

    private int updateResistance(final @NotNull CommandContext<CommandSourceStack> context, final Supplier<DamageResistant> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        final DamageResistant update = function.get();

        if (stack.hasData(DataComponentTypes.DAMAGE_RESISTANT)) {
            final DamageResistant resistant = stack.getData(DataComponentTypes.DAMAGE_RESISTANT);
            if (resistant.types().key().equals(update.types().key())) {
                stack.unsetData(DataComponentTypes.DAMAGE_RESISTANT);
                Utils.tell(context, "<primary>Your item is no longer resistant to <secondary>" + update.types().key() + "</secondary>.");
                return 1;
            }
        }

        stack.setData(DataComponentTypes.DAMAGE_RESISTANT, update);
        Utils.tell(context, "<primary>Your item is now resistant to <secondary>" + update.types().key() + "</secondary>.");
        return 1;
    }

    private int handleView(final @NotNull CommandContext<CommandSourceStack> context) {
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

        Utils.tell(user, "<primary>Your item is <secondary>resistant to " + (meta.hasDamageResistant() ? meta.getDamageResistant().key().asMinimalString() :
                "<red>nothing</red>") + "</secondary>!");
        return 1;
    }

}
