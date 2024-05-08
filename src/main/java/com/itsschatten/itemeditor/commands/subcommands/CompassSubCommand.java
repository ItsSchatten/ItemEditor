package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CompassSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public CompassSubCommand(@NotNull CommandBase owningCommand) {
        super("compass", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><amount></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set a compass' tracked location.
                \s
                ◼ <secondary><x><optional> <y><optional> <z><optional></secondary> Set the position the compass is tracking.
                ◼ <secondary>[-clear]<optional></secondary> Clear the tracked position.
                ◼ <secondary>[-view]<optional></secondary> View the tracked position.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        if (!(stack.getItemMeta() instanceof final CompassMeta meta)) {
            returnTell("<red>You are not holding a compass!");
            return;
        }

        if (args.length == 0) {
            meta.setLodestone(user.getLocation());
            meta.setLodestoneTracked(false);
            stack.setItemMeta(meta);

            tell("<primary>Updated your item's tracked location to <secondary>" + StringUtil.prettifyLocation(user.getLocation()) + "</secondary>!");
            return;
        }

        if (args[0].equalsIgnoreCase("-view")) {
            if (meta.hasLodestone()) {
                tell("<primary>Your compass' location is currently: <secondary>" + StringUtil.prettifyLocation(Objects.requireNonNull(meta.getLodestone())) + "</secondary>!");
            } else {
                tell("<primary>Your compass doesn't have a tracked location!");

            }
            return;
        }

        if (args[0].equalsIgnoreCase("-clear")) {
            meta.setLodestone(null);
            stack.setItemMeta(meta);

            tell("<primary>Cleared your compass' tracked location!");
            return;
        }

        final int x = getNumber(0, "<yellow>" + args[0] + "<red> is not a number!");
        final int y = getNumber(1, "<yellow>" + args[1] + "<red> is not a number!");
        final int z = getNumber(2, "<yellow>" + args[2] + "<red> is not a number!");

        final Location location = new Location(user.getWorld(), x, y, z);
        meta.setLodestone(location);
        meta.setLodestoneTracked(false);
        stack.setItemMeta(meta);

        tell("<primary>Updated your item's tracked location to <secondary>" + StringUtil.prettifyLocation(user.getLocation()) + "</secondary>!");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender) && sender instanceof Player player) {
            if (args.length == 1) {
                return Stream.of(player.getLocation().getBlockX(), "-clear", "-view").map(Objects::toString).filter(name -> name.toLowerCase().contains(args[0].toLowerCase())).toList();
            }

            if (args.length == 2) {
                return Stream.of(player.getLocation().getBlockY()).map(Objects::toString).filter(name -> name.toLowerCase().contains(args[1].toLowerCase())).toList();
            }

            if (args.length == 3) {
                return Stream.of(player.getLocation().getBlockZ()).map(Objects::toString).filter(name -> name.toLowerCase().contains(args[2].toLowerCase())).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
