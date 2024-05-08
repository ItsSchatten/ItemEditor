package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TypeSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public TypeSubCommand(@NotNull CommandBase owningCommand) {
        super("type", List.of("material"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><material></secondary>").hoverEvent(StringUtil.color("""
                <primary>Changes the material of your item.
                \s
                ◼ <secondary><material><required></secondary> The material to update the item to.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
            returnTell("<red>Please supply a material to update your item to.");
            return;
        }

        // Get a material.
        final Material material = Material.matchMaterial(args[0]);
        if (material == null) {
            returnTell("<red>The material you specified does not exist.");
            return;
        }

        // Make sure it's an item material.
        if (!material.isItem()) {
            returnTell("<red>The material you specified is not a item.");
            return;
        }

        // Update the item to a copy of the current stack with the new material.
        user.getInventory().setItemInMainHand(stack.withType(material));
        tell("<primary>Updated your item's material to <secondary>" + material.getKey().getKey() + "</secondary>!");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Arrays.stream(Material.values()).map((mat) -> mat.getKey().getKey()).map(String::toLowerCase)
                        .filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
