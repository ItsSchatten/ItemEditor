package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ArmorTrimSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public ArmorTrimSubCommand(@NotNull CommandBase owningCommand) {
        super("armortrim", List.of("trim"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><trim> <material></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the armor trim on an armor item.
                \s
                ◼ <secondary><trim><required></secondary> The armor trim to apply.
                ◼ <secondary><material><required></secondary> The armor trim material.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        if (!(stack.getItemMeta() instanceof final ArmorMeta meta)) {
            returnTell("<red>You are not holding a piece of armor that can have trim applied to it!");
            return;
        }

        if (args.length == 0) {
            returnTell("<red>Please specify an armor trim!");
            return;
        }

        if (args.length == 1) {
            returnTell("<red>Please specify a trim material!");
            return;
        }

        // Attempt to get a pattern key.
        final NamespacedKey patternKey = args[0].toLowerCase().contains(":") ? NamespacedKey.fromString(args[0].toLowerCase()) : NamespacedKey.minecraft(args[0].toLowerCase());
        if (patternKey == null) {
            returnTell("<yellow>" + args[0] + " <red>is not a valid trim!");
            return;
        }

        // Attempt to get a trim material key.
        final NamespacedKey trimKey = args[1].toLowerCase().contains(":") ? NamespacedKey.fromString(args[1].toLowerCase()) : NamespacedKey.minecraft(args[1].toLowerCase());
        if (trimKey == null) {
            returnTell("<yellow>" + args[1] + " <red>is not a valid trim material!");
            return;
        }

        // Get a trim pattern from the internal registry.
        final TrimPattern trimPattern = Registry.TRIM_PATTERN.get(patternKey);
        if (trimPattern == null) {
            returnTell("<yellow>" + args[0] + " <red>is not a valid trim!");
            return;
        }

        // Get the trim material from the internal registry.
        final TrimMaterial trimMaterial = Registry.TRIM_MATERIAL.get(trimKey);
        if (trimMaterial == null) {
            returnTell("<yellow>" + args[1] + " <red>is not a valid trim material!");
            return;
        }

        final ArmorTrim trim = new ArmorTrim(trimMaterial, trimPattern);
        meta.setTrim(trim);
        stack.setItemMeta(meta);

        tell(StringUtil.color("<primary>Set your armor's trim to").appendSpace().append(trimPattern.description().color(TextColor.fromHexString("#D8D8F6"))).appendSpace()
                .append(StringUtil.color("<primary>with the material ")).append(trimMaterial.description()).append(StringUtil.color("<primary>.")));
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                final List<String> strings = new ArrayList<>(Registry.TRIM_PATTERN.stream().map((trimPattern) -> trimPattern.key().value()).toList());
                strings.add("-clear");
                return strings.stream().filter((name) -> name.contains(args[0].toLowerCase())).toList();
            }

            if (args.length == 2) {
                return Registry.TRIM_MATERIAL.stream().map((trimPattern) -> trimPattern.key().value()).filter((name) -> name.contains(args[0].toLowerCase())).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
