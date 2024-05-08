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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public class MaxStackSizeSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public MaxStackSizeSubCommand(@NotNull CommandBase owningCommand) {
        super("maxstacksize", List.of("maxsize", "maxstack"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><amount></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the max amount of your item.
                <gray><i>Restricted to a value between 1 and 99</i></gray>
                \s
                ◼ <secondary><amount><required></secondary> The amount to update the max stack to.
                ◼ <secondary>[-view]<optional></secondary> View the item's max stack size.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
            returnTell("<red>Please specify a max stack size!");
            return;
        }

        // Reset to default.
        if (args[0].equalsIgnoreCase("null")) {
            meta.setMaxStackSize(null);
            stack.setItemMeta(meta);
            tell("<primary>Reset your item's maximum stack size to <secondary>" + stack.getType().getMaxStackSize() + "</secondary>!");
            return;
        }

        // View max stack size.
        if (args[0].equalsIgnoreCase("-view")) {
            if (meta.hasMaxStackSize()) {
                tell("<primary>Your item's max stack size is custom! Max stack size: <secondary>" + meta.getMaxStackSize() + "</secondary> <gray>(" + stack.getType().getMaxStackSize() + ")</gray>!");
            } else {
                tell("<primary>Your item's max stack size is vanilla. Max stack size: <secondary>" + stack.getType().getMaxStackSize() + "</secondary>!");
            }
            return;
        }

        // Get an amount.
        final int amount = getNumber(0, "<yellow>" + args[0] + "<red> is not a number!");

        meta.setMaxStackSize(Math.max(Math.min(amount, 99), 0));
        stack.setItemMeta(meta);
        tell("<primary>Updated your item's maximum stack size to <secondary>" + amount + "</secondary>!");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                final List<String> strings = new ArrayList<>(IntStream.range(1, 99).mapToObj(Integer::toString).toList());
                strings.add("null");
                strings.add("-view");

                return strings.stream().filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
