package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public class EnchantmentGlintSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public EnchantmentGlintSubCommand(@NotNull CommandBase owningCommand) {
        super("glint", List.of("enchantmentglint", "glow"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><true|false|yes|no|remove></secondary>").hoverEvent(StringUtil.color("""
                <primary>Overrides the enchantment glint on your item.
                <info>Defaults to toggling between on and off if no arguments.</info>
                \s
                ◼ <secondary><remove><optional></secondary> Remove the enchantment glint override from the item.
                ◼ <secondary><true|false|yes|no><required></secondary> Set the enchantment glint override.
                ◼ <secondary>[-view]<optional></secondary> View the current enchantment glint override for this item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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

        // If we have no args or if the argument is "remove" just remove the glint override.
        if (args.length == 0) {
            // Set to true if no override, or toggle.
            meta.setEnchantmentGlintOverride(!meta.hasEnchantmentGlintOverride() || !meta.getEnchantmentGlintOverride());
            tell("<primary>Your item's enchantment glint override has been updated to <secondary>" + meta.getEnchantmentGlintOverride() + "</secondary>!");
        } else if (args[0].equalsIgnoreCase("remove")) {
            meta.setEnchantmentGlintOverride(null);
            tell("<primary>Your item's enchantment glint override has been <secondary>removed</secondary>!");
        } else if (args[0].equalsIgnoreCase("-view")) {
            if (!meta.hasEnchantmentGlintOverride()) {
                tell("<primary>Your item does not currently have an override for enchantment glint!");
            } else {
                tell("<primary>Your item's enchantment glint override is <secondary>currently " + meta.getEnchantmentGlintOverride() + "</secondary>!");
            }
            return;
        } else {
            // Get the override.
            // True: if arg is true or yes.
            // False: for anything else.
            final boolean override = Boolean.parseBoolean(args[0].toLowerCase()) || args[0].equalsIgnoreCase("yes");
            meta.setEnchantmentGlintOverride(override);
            tell("<primary>Your item's enchantment glint override has been updated to <secondary>" + override + "</secondary>!");
        }

        stack.setItemMeta(meta);
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("remove", "true", "false", "yes", "no", "-view").filter(name -> name.contains(args[0].toLowerCase())).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
