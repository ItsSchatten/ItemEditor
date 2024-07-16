package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.commands.arguments.EquipmentSlotArgument;
import com.itsschatten.itemeditor.commands.arguments.GenericEnumArgument;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class AttributeSubCommand extends BrigadierCommand {

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
        return Commands.literal("attribute")
                .then(Commands.literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (stack.isEmpty()) {
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
                .then(Commands.literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (stack.isEmpty()) {
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
                                Objects.requireNonNull(meta.getAttributeModifiers()).forEach((attribute, modifier) -> {
                                    Utils.tell(user, "<secondary>◼ <hover:show_text:'" + modifierToString(modifier) + "'>" + attribute.name().toLowerCase().replace("_", " ") + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary>");
                                });
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have any attributes.");
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("attribute", ArgumentTypes.resourceKey(RegistryKey.ATTRIBUTE))
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("operation", GenericEnumArgument.generic(AttributeModifier.Operation.class))
                                                .then(Commands.argument("slot", new EquipmentSlotArgument())
                                                        .executes(context -> {
                                                            final Player user = (Player) context.getSource().getSender();
                                                            // Get the item stack in the user's main hand.
                                                            final ItemStack stack = user.getInventory().getItemInMainHand();
                                                            if (stack.isEmpty()) {
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
                                                    if (stack.isEmpty()) {
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
                                            if (stack.isEmpty()) {
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
                            if (stack.isEmpty()) {
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
                                Objects.requireNonNull(meta.getAttributeModifiers()).forEach((attribute, modifier) -> {
                                    Utils.tell(user, "<secondary>◼ <hover:show_text:'" + modifierToString(modifier) + "'>" + attribute.name().toLowerCase().replace("_", " ") + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary>");
                                });
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have any attributes.");
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("attribute", ArgumentTypes.resourceKey(RegistryKey.ATTRIBUTE))
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (stack.isEmpty()) {
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
                                    Utils.tell(user, "<primary>Removed attribute <secondary>" + attribute.name().toLowerCase().replace("_", " ") + "</secondary> from your item!");
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
        final AttributeModifier modifier = new AttributeModifier(new NamespacedKey("itemeditor", "ie-cmd-" + attribute.name().toLowerCase() + UUID.randomUUID().toString().substring(0, 5)), value, operation, group);


        // Add the attribute.
        meta.addAttributeModifier(attribute, modifier);
        stack.setItemMeta(meta);

        Utils.tell(user, "<primary>Added the attribute <secondary><hover:show_text:'" + modifierToString(modifier) + "'>" +
                attribute.name().toLowerCase().replace("_", " ") + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary> to your item!");
    }

//    @Override
//    protected void run(@NotNull Player user, String[] args) {
//        // We need an Attribute to add or remove!
//        if (args.length == 1) {
//            returnTell("<red>Please provide an attribute to " + (args[0].equalsIgnoreCase("add") ? "add" : "remove") + "!");
//            return;
//        }
//
//        final Attribute attribute = Attribute.valueOf(args[1].toUpperCase());
//
//        if (args[0].equalsIgnoreCase("add")) {
//            // Make sure we have an attribute.
//            if (args.length == 2) {
//                returnTell("<red>Please provide a value for your attribute!");
//                return;
//            }
//
//            // Get an operation, or default to ADD_NUMBER.
//            final AttributeModifier.Operation operation = args.length == 3 ? AttributeModifier.Operation.ADD_NUMBER : AttributeModifier.Operation.valueOf(args[3].toUpperCase());
//            final EquipmentSlotGroup slot = args.length == 4 ? null : EquipmentSlotGroup.getByName(args[4].toUpperCase());
//            // Generate the attribute modifier.
//            final AttributeModifier modifier = new AttributeModifier(NamespacedKey.fromString("itemeditor:ie-cmd-" + attribute.name().toLowerCase()), getDouble(2, "<yellow>" + args[2] + "<red> is not a valid double!"), operation, slot);
//
//            // Add the attribute.
//            meta.addAttributeModifier(attribute, modifier);
//            tell("<primary>Added the attribute <secondary><hover:show_text:'" + modifierToString(modifier) + "'>" + attribute.name().toLowerCase().replace("_", " ") + " <gray>(<i>Hover for values.</i>)</gray></hover></secondary> to your item!");
//        } else {
//
//        }
//
//        // Update the item.
//        stack.setItemMeta(meta);
//    }
//
//    @Override
//    public List<String> getTabComplete(Player player, String[] args) {
//        if (testPermissionSilent(player)) {
//            if (args.length == 1) {
//                return Stream.of("add", "remove", "-clear").filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
//            }
//
//            if (args.length == 2) {
//                if (args[0].equalsIgnoreCase("remove")) {
//                    if (player.getInventory().getItemInMainHand().getItemMeta().getAttributeModifiers() == null || player.getInventory().getItemInMainHand().getItemMeta().getAttributeModifiers().isEmpty()) {
//                        return super.getTabComplete(player, args);
//                    }
//
//                    return Objects.requireNonNull(player.getInventory().getItemInMainHand().getItemMeta().getAttributeModifiers()).keySet()
//                            .stream().map((attribute) -> attribute.name().toLowerCase()).filter((name) -> name.contains(args[1].toLowerCase())).toList();
//                }
//
//                return Arrays.stream(Attribute.values()).map((attribute) -> attribute.name().toLowerCase()).filter((name) -> name.contains(args[1].toLowerCase())).toList();
//            }
//
//            if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
//                return Arrays.stream(AttributeModifier.Operation.values()).map((operation) -> operation.name().toLowerCase()).filter((name) -> name.contains(args[3].toLowerCase())).toList();
//            }
//
//            if (args.length == 5 && args[0].equalsIgnoreCase("add")) {
//                return Arrays.stream(EquipmentSlot.values()).map((slot) -> slot.name().toLowerCase()).filter((name) -> name.contains(args[4].toLowerCase())).toList();
//
//            }
//
//        }
//
//        return super.getTabComplete(player, args);
//    }

}
