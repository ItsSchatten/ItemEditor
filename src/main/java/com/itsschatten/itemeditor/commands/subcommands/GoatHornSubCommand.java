package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class GoatHornSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public GoatHornSubCommand(@NotNull CommandBase owningCommand) {
        super("goathorn", List.of("goathornsound"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><instrument></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the instrument on your goat horn.
                \s
                ◼ <secondary><instrument><required></secondary> The instrument to set on the goat horn.
                ◼ <secondary>[-view]<optional></secondary> View the item's current instrument.
                ◼ <secondary>[-clear]<optional></secondary> Clear the item's current instrument.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
    }

    @Override
    protected void run(@NotNull Player user, String[] args) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            returnTell("<red>You need to be holding an item in your hand.");
            return;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final MusicInstrumentMeta meta)) {
            returnTell("<red>You are not holding a goat horn!");
            return;
        }

        // Clear goat horn.
        if (args.length == 0 || args[0].equalsIgnoreCase("-clear")) {
            tell("<primary>Cleared the instrument of your goat horn.");
            return;
        }

        if (args[0].equalsIgnoreCase("-view")) {
            if (meta.getInstrument() != null) {
                tell("<primary>Your instrument is currently <secondary>" + Objects.requireNonNull(Registry.INSTRUMENT.getKey(meta.getInstrument())).getKey() + "</secondary>.");
            } else {
                tell("<primary>Your instrument is doesn't anything assinged to it.");
            }
            return;
        }

        // Get the instrument from the registry.
        final NamespacedKey key = args[0].contains(":") ? NamespacedKey.fromString(args[0]) : NamespacedKey.minecraft(args[0]);
        if (key == null) {
            returnTell("<red>Could not find any instrument by the name <yellow>" + args[0] + "</yellow>.");
            return;
        }

        final MusicInstrument instrument = Registry.INSTRUMENT.get(key);
        if (instrument == null) {
            returnTell("<red>Could not find any instrument by the name <yellow>" + args[0] + "</yellow>.");
            return;
        }

        meta.setInstrument(instrument);
        stack.setItemMeta(meta);
        tell("<primary>Set the instrument to <secondary>" + Objects.requireNonNull(Registry.INSTRUMENT.getKey(instrument)).getKey().replace("_", " ") + "</secondary> for your item.");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Registry.INSTRUMENT.stream().map((instrument) -> instrument.key().asString()).filter((name) -> name.toLowerCase().contains(args[0].toLowerCase())).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
