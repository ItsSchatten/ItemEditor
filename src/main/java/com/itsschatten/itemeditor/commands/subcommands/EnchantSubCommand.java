package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class EnchantSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public EnchantSubCommand(@NotNull CommandBase owningCommand) {
        super("enchant", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><enchantment></secondary>").hoverEvent(StringUtil.color("""
                <primary>Add and remove enchantments.
                \s
                ◼ <secondary><enchantment><required></secondary> The enchantment to add.
                ◼ <secondary><level><optional></secondary> The level of the enchantment, set to 0 to remove the enchantment.
                ◼ <secondary>[-view]<optional></secondary> View all enchantments and their levels.""").asHoverEvent());
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
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            returnTell("<red>For some reason the item's meta is null!");
            return;
        }

        if (args.length == 0) {
            returnTell("<red>Please provide an enchantment name.");
            return;
        }

        // Send a list of the enchantments on the item.
        if (args[0].equalsIgnoreCase("-view")) {
            if (!meta.hasEnchants()) {
                tell("<primary>Your item is not enchanted.");
                return;
            }

            tell("<primary>Your item currently has the following enchantments: <#D8D8F6>" + String.join("<gray>,</gray> ", meta.getEnchants().entrySet().stream()
                    .map((entry) -> "<click:suggest_command:'/ie enchant " + entry.getKey().getKey().getKey() + " 0'><hover:show_text:'<gray><i>Click to suggest the command to remove this enchantment!'>" + entry.getKey().getKey().getKey().replace("_", " ") + " " + entry.getValue() + "</hover></click>").toList()));
            tell("<gray><i>Click an enchantment above to suggest the command to remove it!");
            return;
        }

        // Get the enchantment from the registry.
        final NamespacedKey enchantmentKey = args[0].contains(":") ? NamespacedKey.fromString(args[0]) : NamespacedKey.minecraft(args[0]);
        if (enchantmentKey == null) {
            returnTell("<red>Could not find any enchantment by the name <yellow>" + args[0] + "</yellow>.");
            return;
        }
        final Enchantment enchantment = Registry.ENCHANTMENT.get(enchantmentKey);
        if (enchantment == null) {
            returnTell("<red>Could not find any enchantment by the name <yellow>" + args[0] + "</yellow>.");
            return;
        }

        if (args.length >= 2) {
            // Get a level for the enchantment.
            final int level = getNumber(1, "<yellow>" + args[1] + "</yellow><red> is not a valid number!");
            // Check if we want to remove an enchantment.
            if (level <= 0) {
                // Remove our enchantment.
                meta.removeEnchant(enchantment);
                tell("<primary>Removed the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> from your item.");
            } else {
                // Add our enchantment with the provided level.
                meta.addEnchant(enchantment, level, true);
                tell("<primary>Added the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> at level <secondary>" + level + "</secondary> to your item.");
            }
        } else {
            // Add our enchantment at level 1.
            meta.addEnchant(enchantment, 1, true);
            tell("<primary>Added the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> to your item.");
        }

        // Finally, update the item meta.
        stack.setItemMeta(meta);
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                final List<String> strings = new ArrayList<>(Registry.ENCHANTMENT.stream().map((enchant) -> enchant.getKey().getKey()).toList());
                strings.add("-view");
                return strings.stream().filter((name) -> name.contains(args[0].toLowerCase())).toList();
            }

            if (args.length == 2) {
                // Get the enchantment.
                final NamespacedKey enchantmentKey = args[0].contains(":") ? NamespacedKey.fromString(args[0]) : NamespacedKey.minecraft(args[0]);
                if (enchantmentKey == null) {
                    return super.getTabComplete(sender, args);
                }
                final Enchantment enchantment = Registry.ENCHANTMENT.get(enchantmentKey);
                if (enchantment == null) {
                    return super.getTabComplete(sender, args);
                }

                return IntStream.rangeClosed(0, enchantment.getMaxLevel()).mapToObj(Integer::toString).filter((name) -> name.contains(args[1].toLowerCase())).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
