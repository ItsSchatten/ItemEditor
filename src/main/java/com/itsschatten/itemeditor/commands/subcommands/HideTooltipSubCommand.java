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

public class HideTooltipSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public HideTooltipSubCommand(@NotNull CommandBase owningCommand) {
        super("hidetooltip", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><true|false|yes|no></secondary>").hoverEvent(StringUtil.color("""
                <primary>Hides the item's entire tooltip when hovered in an inventory..
                \s
                ◼ <secondary><true|false|yes|no><optional></secondary> Set whether to hide the tooltip or not.
                ◼ <secondary>[-view]<optional></secondary> View the item's current hide tooltip status.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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

        // Toggle hide tooltip.
        if (args.length == 0) {
            meta.setHideTooltip(!meta.isHideTooltip());
            stack.setItemMeta(meta);
            tell("<primary>Your item's tooltip is <secondary>" + (meta.isHideTooltip() ? "now" : "no longer") + "</secondary> hidden!");
            return;
        }

        // View the current hide tooltip status.
        if (args[0].equalsIgnoreCase("-view")) {
            tell("<primary>Your item's full tooltip is <secondary>currently" + (meta.isHideTooltip() ? "" : " not") + "</secondary> hidden!");
            return;
        }

        final boolean value = Boolean.parseBoolean(args[0]) || args[0].equalsIgnoreCase("yes");
        // No change required.
        if (meta.isHideTooltip() == value) {
            tell("<primary>Your item's tooltip is <secondary>already" + (meta.isHideTooltip() ? "" : " not") + "</secondary> hidden!");
            return;
        }

        // Set it the value.
        meta.setHideTooltip(value);
        stack.setItemMeta(meta);
        tell("<primary>Your item's tooltip is <secondary>" + (meta.isHideTooltip() ? "now" : "no longer") + "</secondary> hidden!");
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
