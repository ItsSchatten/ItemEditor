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
import java.util.Locale;
import java.util.stream.Stream;

public class FireResistantSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public FireResistantSubCommand(@NotNull CommandBase owningCommand) {
        super("fireresistant", List.of("fireresist"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><true|false|yes|no></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set whether your item is fire resistant or not.
                \s
                ◼ <secondary><true|false|yes|no><optional></secondary> Set whether your item will burn in lava or fire.
                ◼ <secondary>[-view]<optional></secondary> View the item's current fire resistance status.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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

        // Toggle fire resistance.
        if (args.length == 0) {
            meta.setFireResistant(!meta.isFireResistant());
            stack.setItemMeta(meta);
            tell("<primary>Your item is <secondary>" + (meta.isFireResistant() ? "now" : "no longer") + "</secondary> fire resistant!");
            return;
        }

        // View the current fire resistance.
        if (args[0].equalsIgnoreCase("-view")) {
            tell("<primary>Your item is <secondary>currently" + (meta.isFireResistant() ? "" : " not") + "</secondary> fire resistant!");
            return;
        }

        final boolean value = Boolean.parseBoolean(args[0]) || args[0].equalsIgnoreCase("yes");
        // No change required.
        if (meta.isFireResistant() == value) {
            tell("<primary>Your item is <secondary>already" + (meta.isFireResistant() ? "" : " not") + "</secondary> fire resistant!");
            return;
        }

        // Set it the value.
        meta.setFireResistant(value);
        stack.setItemMeta(meta);
        tell("<primary>Your item is <secondary>" + (meta.isFireResistant() ? "now" : "no longer") + "</secondary> fire resistant!");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("true", "false", "yes", "no", "-view").filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
