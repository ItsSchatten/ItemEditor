package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class AttributeSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public AttributeSubCommand(@NotNull CommandBase owningCommand) {
        super("attribute", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><add|remove|-clear|-view> [attribute] [value] ...</secondary>").hoverEvent(StringUtil.color("""
                <primary>Add or remove an attribute from an item.
                \s
                ◼ <secondary><add><required> <value><required> <operation><optional> <slot><optional></secondary> Add an attribute to the item, with the optional operation (defaulting to ADD_NUMBER) and when in a specific slot (defaults to any).
                ◼ <secondary><remove><required></secondary> Remove an attribute from the item.
                ◼ <secondary><-clear><required></secondary> Clear all attributes on the item.
                ◼ <secondary><-view><optional></secondary> View all attributes on the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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

        // We need an argument!
        if (args.length == 0) {
            returnTell("<red>Please specify if you wish to add, remove, or clear all attributes!");
            return;
        }

        // Oh, we're clearing, clear the attributes.
        if (args[0].equalsIgnoreCase("-clear")) {
            meta.setAttributeModifiers(null);
            stack.setItemMeta(meta);
            tell("<primary>Cleared all attributes from your item!");
            return;
        }

        if (args[0].equalsIgnoreCase("-view")) {
            // We have attributes.
            if (meta.hasAttributeModifiers()) {
                tell("<primary>Your item has the following attributes:");
                Objects.requireNonNull(meta.getAttributeModifiers()).forEach((attribute, modifier) -> {
                    tell("<secondary>◼ <hover:show_text:'" + modifierToString(modifier) + "'>" + attribute.name().toLowerCase().replace("_", " ") + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary>");
                });
            } else {
                tell("<primary>Your item doesn't have any attributes.");
            }
            return;
        }

        // We need an Attribute to add or remove!
        if (args.length == 1) {
            returnTell("<red>Please provide an attribute to " + (args[0].equalsIgnoreCase("add") ? "add" : "remove") + "!");
            return;
        }

        final Attribute attribute = Attribute.valueOf(args[1].toUpperCase());

        if (args[0].equalsIgnoreCase("add")) {
            // Make sure we have an attribute.
            if (args.length == 2) {
                returnTell("<red>Please provide a value for your attribute!");
                return;
            }

            // Get an operation, or default to ADD_NUMBER.
            final AttributeModifier.Operation operation = args.length == 3 ? AttributeModifier.Operation.ADD_NUMBER : AttributeModifier.Operation.valueOf(args[3].toUpperCase());
            final EquipmentSlot slot = args.length == 4 ? null : EquipmentSlot.valueOf(args[4].toUpperCase());
            // Generate the attribute modifier.
            final AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), attribute.name().toLowerCase(), getDouble(2, "<yellow>" + args[2] + "<red> is not a valid double!"), operation, slot);

            // Add the attribute.
            meta.addAttributeModifier(attribute, modifier);
            tell("<primary>Added the attribute <secondary><hover:show_text:'" + modifierToString(modifier) + "'>" + attribute.name().toLowerCase().replace("_", " ") + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary> to your item!");
        } else {
            // Remove our attribute.
            meta.removeAttributeModifier(attribute);
            tell("<primary>Removed attribute <secondary>" + attribute.name().toLowerCase().replace("_", " ") + "</secondary> from your item!");
        }

        // Update the item.
        stack.setItemMeta(meta);
    }

    @Contract(pure = true)
    private @NotNull String modifierToString(final @NotNull AttributeModifier modifier) {
        return """
                <primary>Modifier Name: <secondary>{name}</secondary>
                <primary>Operation: <secondary>{operation}</secondary>
                <primary>Value: <secondary>{value}</secondary>
                <primary>When On: <secondary>{on}</secondary>"""
                .replace("{name}", modifier.getName())
                .replace("{operation}", modifier.getOperation().name().toLowerCase().replace("_", " "))
                .replace("{value}", modifier.getAmount() + "")
                .replace("{on}", modifier.getSlotGroup().toString().replace("_", " "))
                ;
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("add", "remove", "-clear").filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("remove") && (sender instanceof final Player player)) {
                    if (player.getInventory().getItemInMainHand().getItemMeta().getAttributeModifiers() == null || player.getInventory().getItemInMainHand().getItemMeta().getAttributeModifiers().isEmpty()) {
                        return super.getTabComplete(sender, args);
                    }

                    return Objects.requireNonNull(player.getInventory().getItemInMainHand().getItemMeta().getAttributeModifiers()).keySet()
                            .stream().map((attribute) -> attribute.name().toLowerCase()).filter((name) -> name.contains(args[1].toLowerCase())).toList();
                }

                return Arrays.stream(Attribute.values()).map((attribute) -> attribute.name().toLowerCase()).filter((name) -> name.contains(args[1].toLowerCase())).toList();
            }

            if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
                return Arrays.stream(AttributeModifier.Operation.values()).map((operation) -> operation.name().toLowerCase()).filter((name) -> name.contains(args[3].toLowerCase())).toList();
            }

            if (args.length == 5 && args[0].equalsIgnoreCase("add")) {
                return Arrays.stream(EquipmentSlot.values()).map((slot) -> slot.name().toLowerCase()).filter((name) -> name.contains(args[4].toLowerCase())).toList();

            }

        }

        return super.getTabComplete(sender, args);
    }

}
