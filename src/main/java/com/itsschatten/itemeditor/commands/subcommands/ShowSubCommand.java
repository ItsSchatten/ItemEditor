package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ShowSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public ShowSubCommand(@NotNull CommandBase owningCommand) {
        super("show", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><flag></secondary>").hoverEvent(StringUtil.color("""
                <primary>Removes an item flag from your item.
                \s
                ◼ <secondary><flag><required></secondary> The item flag to remove.
                ◼ <secondary>[-view]<optional></secondary> View all your item's current flags.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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

        if (args.length == 0 || args[0].equalsIgnoreCase("-view")) {
            if (meta.getItemFlags().isEmpty()) {
                tell("<primary>Your item doesn't have any item flags.");
                return;
            }

            tell("<primary>Your item currently has the following item flags: <#D8D8F6>" + String.join("<gray>,</gray> ", meta.getItemFlags().stream()
                    .map((flag) -> "<click:suggest_command:'/ie show " + flag.name().toLowerCase() + "'><hover:show_text:'<gray><i>Click to suggest the command to remove this flag!'>" + flag.name().toLowerCase().replace("_", " ") + "</hover></click>").toList()));
            tell("<gray><i>Click a flag above to suggest the command to remove it!");
            return;
        }

        try {
            // Find the ItemFlag.
            final ItemFlag flag = ItemFlag.valueOf(args[0]);

            if (!meta.hasItemFlag(flag)) {
                tell("<red>Your item does not have the item flag: <yellow>" + flag.name());
                return;
            }

            // Add the item flag.
            meta.removeItemFlags(flag);
            stack.setItemMeta(meta);

            tell("<primary>Removed <secondary>" + flag.name().toLowerCase().replace("_", " ") + "</secondary> item flag from your item.");
        } catch (final IllegalArgumentException ex) {
            returnTell("<yellow>" + args[0] + "<red> is not a valid item flag.");
        }
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                if (sender instanceof Player player) {
                    final List<String> strings = new ArrayList<>(player.getInventory().getItemInMainHand().getItemFlags().stream().map(ItemFlag::name).map(String::toLowerCase).toList());
                    strings.add("-view");
                    return strings.stream().filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
                }
            }
        }

        return super.getTabComplete(sender, args);
    }
}