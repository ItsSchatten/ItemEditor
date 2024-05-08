package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class RepairCostSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public RepairCostSubCommand(@NotNull CommandBase owningCommand) {
        super("repaircost", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><cost></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets the repair cost of your item.
                \s
                ◼ <secondary><cost><required></secondary> The repair cost penalty for this item.
                ◼ <secondary>[-view]<optional></secondary> See the current repair cost.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        if (!(stack.getItemMeta() instanceof final Repairable meta)) {
            returnTell("<red>Your item cannot be repaired in an anvil!");
            return;
        }

        // We need arguments.
        if (args.length == 0) {
            returnTell("<red>Please provide the repair cost to apply to your item.");
            return;
        }

        // View the current repair cost.
        if (Arrays.stream(args).toList().contains("-view")) {
            tell("<primary>Your item's repair cost is currently <secondary>" + meta.getRepairCost() + "</secondary>.");
            return;
        }

        // Set the repair cost.
        final int repairCost = getNumber(0, "<yellow>" + args[0] + " <red>is not a valid integer.");
        meta.setRepairCost(repairCost);
        stack.setItemMeta(meta);
        tell("<primary>Set the repair cost of your item to <secondary>" + repairCost + "</secondary>.");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("repair", "add", "set", "remove").filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
