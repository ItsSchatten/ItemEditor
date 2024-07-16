package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.MusicInstrument;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class GoatHornSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie horn <secondary><instrument></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the instrument on your goat horn.
                \s
                ◼ <secondary><instrument><required></secondary> The instrument to set on the goat horn.
                ◼ <secondary>[-view]<optional></secondary> View the item's current instrument.
                ◼ <secondary>[-clear]<optional></secondary> Clear the item's current instrument.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie horn "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("horn")
                .then(Commands.literal("-view")
                        .executes(this::handleView)
                )
                .then(Commands.literal("-clear")
                        .executes(context -> updateInstrument(context, (meta) -> {
                            meta.setInstrument(null);
                            Utils.tell(context.getSource(), "<primary>Cleared the instrument of your goat horn.");
                            return meta;
                        }))
                )
                .then(Commands.argument("instrument", ArgumentTypes.resource(RegistryKey.INSTRUMENT))
                        .executes(context -> updateInstrument(context, meta -> {
                            final MusicInstrument instrument = context.getArgument("instrument", MusicInstrument.class);
                            meta.setInstrument(instrument);

                            Utils.tell(context.getSource(), "<primary>Set the instrument to <secondary>" + Objects.requireNonNull(Registry.INSTRUMENT.getKey(instrument)).getKey().replace("_", " ") + "</secondary> for your item.");
                            return meta;
                        }))
                );
    }

    private int handleView(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(context.getSource(), "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final MusicInstrumentMeta meta)) {
            Utils.tell(context.getSource(), "<red>You are not holding a goat horn!");
            return 0;
        }

        if (meta.getInstrument() != null) {
            Utils.tell(user, "<primary>Your instrument is currently <secondary>" + Objects.requireNonNull(Registry.INSTRUMENT.getKey(meta.getInstrument())).getKey() + "</secondary>.");
        } else {
            Utils.tell(user, "<primary>Your instrument is doesn't anything assinged to it.");
        }

        return 1;
    }

    private int updateInstrument(final @NotNull CommandContext<CommandSourceStack> context, final Function<MusicInstrumentMeta, MusicInstrumentMeta> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(context.getSource(), "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final MusicInstrumentMeta meta)) {
            Utils.tell(context.getSource(), "<red>You are not holding a goat horn!");
            return 0;
        }

        stack.setItemMeta(function.apply(meta));
        return 1;
    }
}
