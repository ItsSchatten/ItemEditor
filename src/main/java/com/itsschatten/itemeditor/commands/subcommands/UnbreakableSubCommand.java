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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class UnbreakableSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public UnbreakableSubCommand(@NotNull CommandBase owningCommand) {
        super("unbreakable", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><true|false|yes|no></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets an item to be unbreakable or not.
                \s
                ◼ <secondary><true|false|yes|no><optional></secondary> true/false to determine if it's unbreakable or not, if not provided it will toggle the status.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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

        // Toggle unbreakable.
        if (args.length < 1) {
            meta.setUnbreakable(!meta.isUnbreakable());
            stack.setItemMeta(meta);
            tell("<primary>Your item is <secondary>" + (meta.isUnbreakable() ? "now" : "no longer") + "</secondary> unbreakable!");
            return;
        }

        if (args[0].equalsIgnoreCase("-view")) {
            tell("<primary>Your item is <secondary>currently" + (meta.isUnbreakable() ? "" : " not") + "</secondary> unbreakable!");
            return;
        }

        final boolean value = Boolean.parseBoolean(args[0]) || args[0].equalsIgnoreCase("yes");
        // No change required.
        if (meta.isUnbreakable() == value) {
            tell("<primary>Your item is <secondary>already" + (meta.isUnbreakable() ? "" : " not") + "</secondary> unbreakable!");
            return;
        }

        // Set it the value.
        meta.setUnbreakable(value);
        stack.setItemMeta(meta);
        tell("<primary>Your item is <secondary>" + (meta.isUnbreakable() ? "now" : "no longer") + "</secondary> unbreakable!");
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
