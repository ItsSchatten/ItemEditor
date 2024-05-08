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

public class CustomModelDataSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public CustomModelDataSubCommand(@NotNull CommandBase owningCommand) {
        super("custommodeldata", List.of("modeldata", "model"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><data></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the custom model data of the item.
                <gray><i>Pass null to remove the custom data.</i></gray>
                \s
                ◼ <secondary><data><required></secondary> The number to assign as the custom model data.
                ◼ <secondary>[-view]<optional></secondary> View the item's current custom model data.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
            returnTell("<red>Please specify a number for the custom model data!");
            return;
        }

        // View the data.
        if (args[0].equalsIgnoreCase("-view")) {
            if (meta.hasCustomModelData()) {
                tell("<primary>Your item's custom model data is <secondary>" + meta.getCustomModelData() + "</secondary>!");
            } else {
                tell("<primary>Your item doesn't have any custom model data!");
            }
            return;
        }

        // Remove data.
        if (args[0].equalsIgnoreCase("null")) {
            meta.setCustomModelData(null);
            stack.setItemMeta(meta);
            tell("<primary>Cleared your item's custom model data!");
            return;
        }

        // Get an amount.
        final int data = getNumber(0, "<yellow>" + args[0] + "<red> is not a number!");

        meta.setCustomModelData(data);
        stack.setItemMeta(meta);
        tell("<primary>Updated your item's custom model data to <secondary>" + data + "</secondary>!");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("null", "-view").filter((name) -> name.contains(args[0].toLowerCase())).toList();
            }
        }
        return super.getTabComplete(sender, args);
    }
}
