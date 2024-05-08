package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class DurabilitySubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public DurabilitySubCommand(@NotNull CommandBase owningCommand) {
        super("durability", List.of("damage"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><repair|add|set|remove|damage #></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets a damageable item's damage..
                \s
                ◼ <secondary><damage #><required></secondary> The damage value.
                ◼ <secondary><repair><first></secondary> Repair the item.
                ◼ <secondary><add><first></secondary> Add the damage to the item.
                ◼ <secondary><set><first></secondary> Set the damage of the item.
                ◼ <secondary><remove><first></secondary> Remove the damage from the item.
                ◼ <secondary>[-view]<optional></secondary> See the current damage.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        if (!(stack.getItemMeta() instanceof final Damageable meta)) {
            returnTell("<red>Your item cannot be damaged!");
            return;
        }

        // We need arguments.
        if (args.length == 0) {
            returnTell("<red>Please provide the amount of damage to apply to your item.");
            return;
        }

        // Check if we have can have durability.
        if (stack.getType().getMaxDurability() == 0 && !meta.hasMaxDamage()) {
            returnTell("<red>Your item doesn't have a custom max damage and doesn't have damage by default.");
            return;
        }

        if (meta.isUnbreakable()) {
            returnTell("<red>Your item is unbreakable and cannot be damaged!");
            return;
        }

        // View the current item damage.
        if (Arrays.stream(args).toList().contains("-view")) {
            if (meta.hasMaxDamage()) {
                tell("<primary>Your item's damage is currently <secondary>" + meta.getDamage() + "</secondary><gray>/" + meta.getMaxDamage() + " (<hover:show_text:'<primary>Default: <secondary>" + stack.getType().getMaxDurability() + "</secondary>'><i>custom max damage</i></hover>)</gray>.");
            } else {
                tell("<primary>Your item's damage is currently <secondary>" + meta.getDamage() + "</secondary><gray>/" + stack.getType().getMaxDurability() + "</gray>.");
            }
            return;
        }

        if (args.length == 1) {
            // See if we want to repair.
            if (args[0].equalsIgnoreCase("repair")) {
                meta.setDamage(0);
                stack.setItemMeta(meta);
                tell("<primary>Your item has been repaired.");
                return;
            }

            // Default to setting the damage.
            final int damage = getNumber(0, "<yellow>" + args[0] + " <red>is not a valid integer.");
            meta.setDamage(damage);
            stack.setItemMeta(meta);
            tell("<primary>Set the damage of your item to <secondary>" + damage + "</secondary><gray>/" + (meta.hasMaxDamage() ? meta.getMaxDamage() : stack.getType().getMaxDurability()) + "</gray>.");
            return;
        }

        // Get the damage to be set.
        final int damage = getNumber(1, "<yellow>" + args[1] + " <red>is not a valid integer.");

        switch (args[0].toLowerCase()) {
            case "add" -> {
                // Adds the damage to the current damage.
                meta.setDamage(damage + meta.getDamage());
                tell("<primary>Added <secondary>" + damage + "</secondary> damage to your item.");
            }
            case "set" -> {
                // Sets the damage.
                meta.setDamage(damage);
                tell("<primary>Set the damage of your item to <secondary>" + damage + "</secondary><gray>/" + (meta.hasMaxDamage() ? meta.getMaxDamage() : stack.getType().getMaxDurability()) + "</gray>.");
            }
            case "remove" -> {
                // Removes the damage from the current damage.
                meta.setDamage(meta.getDamage() - damage);
                tell("<primary>Removed <secondary>" + damage + "</secondary> damage to your item.");
            }
        }

        // Set the item's meta.
        stack.setItemMeta(meta);
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("repair", "add", "set", "remove").filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }

            if (args.length == 2) {
                if (!args[0].equalsIgnoreCase("repair") && sender instanceof Player player) {
                    return Stream.of("10", "150", "200", player.getInventory().getItemInMainHand().getType().getMaxDurability() + "").filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
                }
            }

        }

        return super.getTabComplete(sender, args);
    }
}
