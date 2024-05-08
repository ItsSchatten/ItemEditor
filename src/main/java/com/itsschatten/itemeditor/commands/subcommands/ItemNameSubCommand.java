package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class ItemNameSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public ItemNameSubCommand(@NotNull CommandBase owningCommand) {
        super("itemname", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><name></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the item's default name.
                \s
                ◼ <secondary><name><required> </secondary> The name for the item.
                ◼ <secondary>[-view]<optional></secondary> View the item's current name.
                ◼ <secondary>[-clear]<optional></secondary> Clear the name.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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

        // Clear the display name if the argument is just 'clear', update the name to nothing if no args, or colorize the arguments.
        Component componentName;
        if (args.length == 1 && args[0].equalsIgnoreCase("-clear")) {
            componentName = null;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("-view")) {
            if (meta.hasItemName()) {
                tell(StringUtil.color("<primary>Your item's name is currently:").appendSpace().append(meta.itemName()).colorIfAbsent(NamedTextColor.WHITE).append(StringUtil.color("<primary>.")));
            } else {
                tell("<primary>Your item doesn't currently have an item name.");
            }
            return;
        } else {
            if (args.length == 0) {
                componentName = Component.text(" ");
            } else {
                final String name = String.join(" ", args);
                componentName = StringUtil.color("<!i>" + name);
            }
        }

        // Finally, update the display name of the item and update it on the item's meta.
        meta.itemName(componentName);
        stack.setItemMeta(meta);

        // Send our success message.
        tell(componentName == null ? StringUtil.color("<primary>Your item's name has been reset to <yellow>" + stack.getType().getKey().getKey().toLowerCase().replace("_", " ") + "</yellow>.")
                : StringUtil.color("<primary>Set your item's name to <reset>'").append(componentName).append(StringUtil.color("<reset>'<primary>.")));
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender) && args.length == 1) {
            return Stream.of("-clear", "-view").filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return super.getTabComplete(sender, args);
    }
}
