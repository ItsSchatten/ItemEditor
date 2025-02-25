package com.itsschatten.itemeditor.commands.subcommands;

import com.google.common.collect.ArrayListMultimap;
import com.itsschatten.itemeditor.commands.arguments.EquipmentSlotArgument;
import com.itsschatten.itemeditor.commands.arguments.GenericEnumArgument;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.RegistryArgumentExtractor;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

// FIXME: Convert to using a method.

public final class AttributeSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie attribute <secondary><add|remove|-clear|-view> [attribute] [value] ...</secondary>").hoverEvent(StringUtil.color("""
                <primary>Add or remove an attribute from an item.
                \s
                ◼ <secondary><add><required> <value><required> <operation><optional> <slot><optional></secondary> Add an attribute to the item, with the optional operation (defaulting to ADD_NUMBER) and when in a specific slot (defaults to any).
                ◼ <secondary><remove><required></secondary> Remove an attribute from the item.
                ◼ <secondary><-clear><required></secondary> Clear all attributes on the item.
                ◼ <secondary><-view><optional></secondary> View all attributes on the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie attribute "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("attribute")
                .then(literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            meta.setAttributeModifiers(null);
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Cleared all attributes from your item!");
                            return 1;
                        })
                )
                .then(literal("-empty")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            meta.setAttributeModifiers(ArrayListMultimap.create());
                            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            stack.setItemMeta(meta);

                            Utils.tell(user, "<primary>Remove the attribute modifiers from your item!");
                            return 1;
                        })
                )
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            // We have attributes.
                            if (meta.hasAttributeModifiers()) {
                                Utils.tell(user, "<primary>Your item has the following attributes:");
                                Objects.requireNonNull(meta.getAttributeModifiers()).forEach((attribute, modifier) -> Utils.tell(user, "<secondary>◼ <hover:show_text:'" + modifierToString(modifier) + "'>" + attribute.key().asMinimalString() + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary>"));
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have any attributes.");
                            }
                            return 1;
                        })
                )
                .then(literal("add")
                        .then(argument("attribute", ArgumentTypes.resourceKey(RegistryKey.ATTRIBUTE))
                                .then(argument("value", DoubleArgumentType.doubleArg())
                                        .then(argument("operation", GenericEnumArgument.generic(AttributeModifier.Operation.class))
                                                .then(argument("slot", new EquipmentSlotArgument())
                                                        .executes(context -> {
                                                            final Player user = (Player) context.getSource().getSender();
                                                            // Get the item stack in the user's main hand.
                                                            final ItemStack stack = user.getInventory().getItemInMainHand();
                                                            if (ItemValidator.isInvalid(stack)) {
                                                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                                                return 0;
                                                            }

                                                            // Get the item's meta and check if it's null,
                                                            // it really shouldn't be but safety.
                                                            final ItemMeta meta = stack.getItemMeta();
                                                            if (meta == null) {
                                                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                                                return 0;
                                                            }

                                                            final Attribute attribute = Registry.ATTRIBUTE.get(RegistryArgumentExtractor.getTypedKey(context, RegistryKey.ATTRIBUTE, "attribute").key());
                                                            if (attribute == null) {
                                                                Utils.tell(user, "<red>Failed to find an attribute!");
                                                                return 0;
                                                            }

                                                            final double value = DoubleArgumentType.getDouble(context, "value");
                                                            final AttributeModifier.Operation operation = context.getArgument("operation", AttributeModifier.Operation.class);
                                                            final EquipmentSlotGroup slot = context.getArgument("slot", EquipmentSlotGroup.class);

                                                            addAttributeModifier(attribute, value, operation, slot, user, stack, meta);
                                                            return 1;
                                                        })
                                                )
                                                .executes(context -> {
                                                    final Player user = (Player) context.getSource().getSender();
                                                    // Get the item stack in the user's main hand.
                                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                                    if (ItemValidator.isInvalid(stack)) {
                                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                                        return 0;
                                                    }

                                                    // Get the item's meta and check if it's null,
                                                    // it really shouldn't be but safety.
                                                    final ItemMeta meta = stack.getItemMeta();
                                                    if (meta == null) {
                                                        Utils.tell(user, "<red>For some reason the item's meta is null!");
                                                        return 0;
                                                    }

                                                    final Attribute attribute = Registry.ATTRIBUTE.get(RegistryArgumentExtractor.getTypedKey(context, RegistryKey.ATTRIBUTE, "attribute").key());
                                                    if (attribute == null) {
                                                        Utils.tell(user, "<red>Failed to find an attribute!");
                                                        return 0;
                                                    }

                                                    final double value = DoubleArgumentType.getDouble(context, "value");
                                                    final AttributeModifier.Operation operation = context.getArgument("operation", AttributeModifier.Operation.class);

                                                    addAttributeModifier(attribute, value, operation, EquipmentSlotGroup.ANY, user, stack, meta);
                                                    return 1;
                                                })
                                        )
                                        .executes(context -> {
                                            final Player user = (Player) context.getSource().getSender();
                                            // Get the item stack in the user's main hand.
                                            final ItemStack stack = user.getInventory().getItemInMainHand();
                                            if (ItemValidator.isInvalid(stack)) {
                                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                                return 0;
                                            }

                                            // Get the item's meta and check if it's null,
                                            // it really shouldn't be but safety.
                                            final ItemMeta meta = stack.getItemMeta();
                                            if (meta == null) {
                                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                                return 0;
                                            }

                                            final Attribute attribute = Registry.ATTRIBUTE.get(RegistryArgumentExtractor.getTypedKey(context, RegistryKey.ATTRIBUTE, "attribute").key());
                                            if (attribute == null) {
                                                Utils.tell(user, "<red>Failed to find an attribute!");
                                                return 0;
                                            }

                                            final double value = DoubleArgumentType.getDouble(context, "value");

                                            addAttributeModifier(attribute, value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY, user, stack, meta);
                                            return 1;
                                        })
                                )
                        )
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            // We have attributes.
                            if (meta.hasAttributeModifiers()) {
                                Utils.tell(user, "<primary>Your item has the following attributes:");
                                Objects.requireNonNull(meta.getAttributeModifiers()).forEach((attribute, modifier) -> Utils.tell(user, "<secondary>◼ <hover:show_text:'" + modifierToString(modifier) + "'>" + attribute.key().asMinimalString() + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary>"));
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have any attributes.");
                            }
                            return 1;
                        })
                )
                .then(literal("remove")
                        .then(argument("attribute", ArgumentTypes.resourceKey(RegistryKey.ATTRIBUTE))
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    // Get the item's meta and check if it's null, it really shouldn't be but safety.
                                    final ItemMeta meta = stack.getItemMeta();
                                    if (meta == null) {
                                        Utils.tell(user, "<red>For some reason the item's meta is null!");
                                        return 0;
                                    }

                                    final Attribute attribute = Registry.ATTRIBUTE.get(RegistryArgumentExtractor.getTypedKey(context, RegistryKey.ATTRIBUTE, "attribute").key());
                                    if (attribute == null) {
                                        Utils.tell(user, "<red>Failed to find an attribute!");
                                        return 0;
                                    }

                                    // Remove our attribute.
                                    meta.removeAttributeModifier(attribute);
                                    stack.setItemMeta(meta);
                                    Utils.tell(user, "<primary>Removed attribute <secondary>" + attribute.key().asMinimalString() + "</secondary> from your item!");
                                    return 1;
                                })
                        )
                );
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

    @SuppressWarnings("UnstableApiUsage")
    private void addAttributeModifier(final @NotNull Attribute attribute, double value, AttributeModifier.Operation operation, EquipmentSlotGroup group, final Player user, final @NotNull ItemStack stack, final @NotNull ItemMeta meta) {
        final AttributeModifier modifier = new AttributeModifier(new NamespacedKey("itemeditor", "ie-cmd-" + attribute.key().asMinimalString().replace(":", "-") + UUID.randomUUID().toString().substring(0, 5)), value, operation, group);

        // Add the attribute.
        meta.addAttributeModifier(attribute, modifier);
        stack.setItemMeta(meta);

        Utils.tell(user, "<primary>Added the attribute <secondary><hover:show_text:'" + modifierToString(modifier) + "'>" +
                attribute.key().asMinimalString() + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary> to your item!");
    }
}
