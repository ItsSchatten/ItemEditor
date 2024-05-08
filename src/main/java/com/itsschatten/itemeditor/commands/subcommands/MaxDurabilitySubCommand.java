package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public class MaxDurabilitySubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public MaxDurabilitySubCommand(@NotNull CommandBase owningCommand) {
        super("maxdurability", List.of("maxdamage"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><durability></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the max durability of your item.
                \s
                ◼ <secondary><amount><required></secondary> The amount to update the max durability to.
                ◼ <secondary>[-view]<optional></secondary> View the item's max durability.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        if (!(stack.getItemMeta() instanceof final Damageable meta)) {
            returnTell("<red>Your item cannot be damaged!");
            return;
        }

        if (args.length == 0) {
            returnTell("<red>Please specify an amount for your max damage!");
            return;
        }

        // View max stack size.
        if (args[0].equalsIgnoreCase("-view")) {
            if (meta.hasMaxStackSize()) {
                tell("<primary>Your item's max damage is custom! Max damage: <secondary>" + meta.getMaxDamage() + "</secondary> <gray>(" + stack.getType().getMaxDurability() + ")</gray>!");
            } else {
                tell("<primary>Your item's max damage is vanilla. Max damage: <secondary>" + stack.getType().getMaxDurability() + "</secondary>!");
            }
            return;
        }

        // Reset to default.
        if (args[0].equalsIgnoreCase("null")) {
            meta.setMaxDamage(null);
            stack.setItemMeta(meta);
            tell("<primary>Reset your item's maximum durability to <secondary>" + stack.getType().getMaxDurability() + "</secondary>!");
            return;
        }

        // Get an amount.
        final int amount = getNumber(0, "<yellow>" + args[0] + "<red> is not a number!");

        meta.setMaxDamage(amount);
        stack.setItemMeta(meta);
        tell("<primary>Updated your item's max damage to <secondary>" + amount + "</secondary>!");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                final List<String> strings = new ArrayList<>(IntStream.range(1, 100).mapToObj(Integer::toString).toList());
                strings.add("null");
                strings.add("-view");

                return strings.stream().filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
