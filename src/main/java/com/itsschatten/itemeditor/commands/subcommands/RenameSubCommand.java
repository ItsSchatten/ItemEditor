package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class RenameSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public RenameSubCommand(@NotNull CommandBase owningCommand) {
        super("rename", List.of("name", "display", "displayname"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><name></secondary>").hoverEvent(StringUtil.color("""
                <primary>Renames the item.
                \s
                ◼ <secondary><name></secondary><required> The name for the item.
                ◼ <secondary>[-view]<optional></secondary> View the item's current display name.
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
            if (meta.hasDisplayName()) {
                tell(StringUtil.color("<primary>Your item's display name is currently:").appendSpace().append(Objects.requireNonNull(meta.displayName())).colorIfAbsent(NamedTextColor.WHITE).append(StringUtil.color("<primary>.")));
            } else {
                tell("<primary>Your item doesn't currently have a display item name.");
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
        meta.displayName(componentName);
        stack.setItemMeta(meta);

        // Send our success message.
        tell(componentName == null ? StringUtil.color("<primary>Your item's display name has been reset to <white>" + (meta.hasItemName() ? MiniMessage.miniMessage().serialize(meta.itemName()) + " <gray>(<i>custom item name</i>)</gray>" : stack.getType().getKey().getKey().toLowerCase().replace("_", " ")) + "</white>.")
                : StringUtil.color("<primary>Set your item's display name to <reset>'").append(componentName).append(StringUtil.color("<reset>'<primary>.")));
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1 && sender instanceof final Player player) {
                final ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();

                return Stream.of("-clear", "-view", meta.hasDisplayName() ? MiniMessage.miniMessage().serialize(Objects.requireNonNull(meta.displayName())) : "")
                        .filter(String::isBlank)
                        .filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
