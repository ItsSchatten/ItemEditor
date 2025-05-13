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
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.JukeboxSong;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public final class JukeboxSubCommand extends BrigadierCommand {

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie jukebox <secondary><name></secondary>").hoverEvent(StringUtil.color("""
                <primary>
                \s
                ◼ <secondary><name><required> </secondary> The song for the item.
                ◼ <secondary>[-view]<optional></secondary> View the item's current song.
                ◼ <secondary>[-clear]<optional></secondary> Clear the song.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie jukebox "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("jukebox")
                .then(literal("-view")
                        .executes(this::view)
                )
                .then(literal("-clear")
                        .executes(this::clear)
                )
                .then(argument("song", ArgumentTypes.resource(RegistryKey.JUKEBOX_SONG))
                        .executes(context -> updatePlayableComponent(context, (component) -> {
                            final JukeboxSong song = context.getArgument("song", JukeboxSong.class);

                            component.setSong(song);
                            Utils.tell(context.getSource(), "<primary>Updated the playable song on your item to '<secondary>" + song.key().asString() + "</secondary>'!");
                            return component;
                        }))
                );
    }

    private int view(final @NotNull CommandContext<CommandSourceStack> context) {
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

        final JukeboxPlayableComponent component = stack.getItemMeta().getJukeboxPlayable();

        Utils.tell(user, """
                <primary>Song <dark_gray>»</dark_gray> <secondary>{song}</secondary>
                <primary>Hidden <dark_gray>»</dark_gray> <secondary>{hidden}</secondary>"""
                .replace("{song}", component.getSong() == null ? "null" : component.getSong().key().asString())
                .replace("{hidden}", String.valueOf(stack.hasData(DataComponentTypes.TOOLTIP_DISPLAY) && stack.getData(DataComponentTypes.TOOLTIP_DISPLAY).hiddenComponents().contains(DataComponentTypes.JUKEBOX_PLAYABLE))));

        return 1;
    }

    private int clear(final CommandContext<CommandSourceStack> context) {
        return updatePlayableComponent(context, (component) -> {
            Utils.tell(context.getSource(), "<primary>Cleared the playable song from your item!");
            return null;
        });
    }

    private int updatePlayableComponent(final @NotNull CommandContext<CommandSourceStack> context, UnaryOperator<JukeboxPlayableComponent> operator) {
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

        final JukeboxPlayableComponent component = operator.apply(stack.getItemMeta().getJukeboxPlayable());
        meta.setJukeboxPlayable(component);
        stack.setItemMeta(meta);
        return 1;
    }

}
