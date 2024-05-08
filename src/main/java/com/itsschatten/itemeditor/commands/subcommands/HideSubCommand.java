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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HideSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public HideSubCommand(@NotNull CommandBase owningCommand) {
        super("hide", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><flag></secondary>").hoverEvent(StringUtil.color("""
                <primary>Adds an item flag to your item.
                \s
                ◼ <secondary><flag><required></secondary> The item flag to add.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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

        // We require a flag.
        if (args.length == 0) {
            returnTell("<red>Please provide a flag to hide.");
            return;
        }

        try {
            // Find the ItemFlag.
            final ItemFlag flag = ItemFlag.valueOf(args[0].toUpperCase());

            // Add the item flag.
            meta.addItemFlags(flag);
            stack.setItemMeta(meta);

            tell("<primary>Added <secondary>" + flag.name().toLowerCase().replace("_", " ") + "</secondary> item flag to your item.");
        } catch (final IllegalArgumentException ex) {
            returnTell("<yellow>" + args[0] + "<red> is not a valid item flag.");
        }

    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Arrays.stream(ItemFlag.values()).map(ItemFlag::name).map(String::toLowerCase).filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
