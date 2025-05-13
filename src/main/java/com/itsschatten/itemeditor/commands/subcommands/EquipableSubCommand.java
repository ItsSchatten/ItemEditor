package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.commands.arguments.GenericEnumArgument;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.RegistryArgumentExtractor;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public final class EquipableSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie equipable <secondary><slot></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets an item to be enchantable or not with it's enchantability.
                \s
                ◼ <secondary><value><optional></secondary> The enchantibility of the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie equipable "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("equipable")
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (stack.hasData(DataComponentTypes.EQUIPPABLE)) {
                                final Equippable equippable = stack.getData(DataComponentTypes.EQUIPPABLE);
                                Utils.tell(user, "<primary>Your item is <secondary>equipable</secondary> in the slot <secondary>" + equippable.slot() + "</secondary>!");
                                Utils.tell(user, "<dark_aqua>Sound <arrow> <secondary>" + equippable.equipSound().asString());
                                Utils.tell(user, "<dark_aqua>Asset ID <arrow> <secondary>" + (equippable.assetId() == null ? "<red>None" : equippable.assetId().asString()));
                                Utils.tell(user, "<dark_aqua>Camera Overlay <arrow> <secondary>" + (equippable.cameraOverlay() == null ? "<red>None" : equippable.cameraOverlay().asString()));
                                Utils.tell(user, "<dark_aqua>Damage On Hurt <arrow> <secondary>" + equippable.damageOnHurt());
                                Utils.tell(user, "<dark_aqua>Dispensable <arrow> <secondary>" + equippable.dispensable());
                                Utils.tell(user, "<dark_aqua>Swappable <arrow> <secondary>" + equippable.swappable());
                                Utils.tell(user, "<dark_aqua>Allowed Entities <arrow>" + (equippable.allowedEntities() == null ? "<red>None" : ""));
                                if (equippable.allowedEntities() != null)
                                    equippable.allowedEntities().forEach(key -> Utils.tell(context, " <primary>◼ <secondary>" + key.key().asString()));
                            } else {
                                Utils.tell(user, "<primary>Your item is <secondary>not equipable</secondary>!");
                            }

                            return 1;
                        })
                )
                .then(literal("-reset")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            stack.resetData(DataComponentTypes.EQUIPPABLE);
                            Utils.tell(user, "<primary>Reset the equipability of your item!");
                            return 1;
                        })
                )
                .then(literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            stack.unsetData(DataComponentTypes.EQUIPPABLE);
                            Utils.tell(user, "<primary>Your item is no longer equipable!");
                            return 1;
                        })
                )

                .then(literal("slot")
                        .then(argument("value", GenericEnumArgument.generic(EquipmentSlot.class))
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    final EquipmentSlot value = context.getArgument("value", EquipmentSlot.class);
                                    final Equippable equippable = stack.hasData(DataComponentTypes.EQUIPPABLE) ? toBuilder(stack.getData(DataComponentTypes.EQUIPPABLE), value).build() : Equippable.equippable(value).build();
                                    stack.setData(DataComponentTypes.EQUIPPABLE, equippable);
                                    Utils.tell(user, "<primary>Your item is <secondary>now</secondary> equipable in the <secondary>" + value + "</secondary> slot!");
                                    return 1;
                                }))
                )

                .then(literal("sound")
                        .then(literal("-reset")
                                .executes(context -> updateEquipability(context, builder -> {
                                    Utils.tell(context, "<primary>Reset!");
                                    return builder.equipSound(Key.key("item.armor.equip_generic"));
                                })))
                        .then(argument("sound resource", ArgumentTypes.resourceKey(RegistryKey.SOUND_EVENT))
                                .executes(context -> updateEquipability(context, builder -> {
                                    final Key key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.SOUND_EVENT, "sound resource").key();
                                    Utils.tell(context, "<primary>Your item will now play <secondary>" + key.asMinimalString() + "</secondary> when equipped!");
                                    return builder.equipSound(key);
                                }))
                        )
                )
                .then(literal("entities")
                        .then(literal("-clear")
                                .executes(context -> updateEquipability(context, builder -> {
                                    Utils.tell(context, "<primary>Your item will now be equipped by all entities!");
                                    return builder.allowedEntities(null);
                                })))
                        .then(literal("add")
                                .then(argument("entity", ArgumentTypes.resourceKey(RegistryKey.ENTITY_TYPE))
                                        .executes(context -> updateEquipability(context, builder -> {
                                                    final TypedKey<EntityType> key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.ENTITY_TYPE, "entity");
                                                    final List<TypedKey<EntityType>> set = new ArrayList<>(builder.build().allowedEntities() != null ? builder.build().allowedEntities().values() : Collections.emptyList());
                                                    set.add(key);

                                                    Utils.tell(context, "<secondary>" + key.key().asString() + "</secondary> <primary>can now equip your item!");
                                                    return builder.allowedEntities(RegistrySet.keySet(RegistryKey.ENTITY_TYPE, set));
                                                })
                                        )
                                )
                        )
                        .then(literal("remove")
                                .then(argument("entity", ArgumentTypes.resourceKey(RegistryKey.ENTITY_TYPE))
                                        .suggests(this::suggestEntities)
                                        .executes(context -> updateEquipability(context, builder -> {
                                                    final TypedKey<EntityType> key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.ENTITY_TYPE, "entity");
                                                    final List<TypedKey<EntityType>> set = new ArrayList<>(builder.build().allowedEntities() != null ? builder.build().allowedEntities().values() : Collections.emptyList());
                                                    if (set.remove(key)) {
                                                        Utils.tell(context, "<secondary>" + key.key().asString() + "</secondary> <primary>can now equip your item!");
                                                    } else {
                                                        Utils.tell(context, "<red>Your item doesn't contain that entity!");
                                                        return builder;
                                                    }

                                                    return builder.allowedEntities(RegistrySet.keySet(RegistryKey.ENTITY_TYPE, set));
                                                })
                                        )
                                )
                        )
                )
                .then(literal("asset")
                        .then(literal("-clear")
                                .executes(context -> updateEquipability(context, builder -> {
                                    Utils.tell(context, "<primary>Removed the equipment model from your item!");
                                    return builder.assetId(null);
                                })))
                        .then(argument("key", ArgumentTypes.key())
                                .executes(context -> updateEquipability(context, builder -> {
                                    final Key key = context.getArgument("key", Key.class);
                                    Utils.tell(context, "<primary>Your item will now use the model <secondary>" + key.asString() + "</secondary>!");
                                    return builder.assetId(key);
                                }))
                        )
                )
                .then(literal("camera")
                        .then(literal("-clear")
                                .executes(context -> updateEquipability(context, builder -> {
                                    Utils.tell(context, "<primary>Removed the camera overlay from your item!");
                                    return builder.cameraOverlay(null);
                                })))
                        .then(argument("key", ArgumentTypes.key())
                                .executes(context -> updateEquipability(context, builder -> {
                                    final Key key = context.getArgument("key", Key.class);
                                    Utils.tell(context, "<primary>Your item will now overlay the camera with <secondary>" + key.asString() + "</secondary>!");
                                    return builder.cameraOverlay(key);
                                }))
                        )
                )
                .then(literal("damage")
                        .then(argument("value", BoolArgumentType.bool())
                                .executes(context -> updateEquipability(context, builder -> {
                                    final boolean value = BoolArgumentType.getBool(context, "value");
                                    Utils.tell(context, "<primary>Your item will <secondary>" + (value ? "now" : "no longer") + "</secondary> be damaged when hurt while worn!");
                                    return builder.damageOnHurt(value);
                                }))
                        )
                        .executes(context -> updateEquipability(context, builder -> {
                            final boolean value = !builder.build().damageOnHurt();
                            Utils.tell(context, "<primary>Your item will <secondary>" + (value ? "now" : "no longer") + "</secondary> be damaged when hurt while worn!");
                            return builder.damageOnHurt(value);
                        }))
                )
                .then(literal("dispensable")
                        .then(argument("value", BoolArgumentType.bool())
                                .executes(context -> updateEquipability(context, builder -> {
                                    final boolean value = BoolArgumentType.getBool(context, "value");
                                    Utils.tell(context, "<primary>Your item can <secondary>" + (value ? "now" : "no longer") + "</secondary> be dispensed!");
                                    return builder.dispensable(value);
                                }))
                        )
                        .executes(context -> updateEquipability(context, builder -> {
                            final boolean value = !builder.build().dispensable();
                            Utils.tell(context, "<primary>Your item can <secondary>" + (value ? "now" : "no longer") + "</secondary> be dispensed!");
                            return builder.dispensable(value);
                        }))
                )
                .then(literal("swappable")
                        .then(argument("value", BoolArgumentType.bool())
                                .executes(context -> updateEquipability(context, builder -> {
                                    final boolean value = BoolArgumentType.getBool(context, "value");
                                    Utils.tell(context, "<primary>Your item can <secondary>" + (value ? "now" : "no longer") + "</secondary> be swapped!");
                                    return builder.swappable(value);
                                }))
                        )
                        .executes(context -> updateEquipability(context, builder -> {
                            final boolean value = !builder.build().swappable();
                            Utils.tell(context, "<primary>Your item can <secondary>" + (value ? "now" : "no longer") + "</secondary> be swapped!");
                            return builder.swappable(value);
                        }))
                );
    }

    // This doesn't disallow entities that are not added to the list.
    private CompletableFuture<Suggestions> suggestEntities(@NotNull CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            return builder.buildFuture();
        }

        if (stack.hasData(DataComponentTypes.EQUIPPABLE)) {
            final Equippable equip = stack.getData(DataComponentTypes.EQUIPPABLE);
            equip.allowedEntities().forEach(key -> builder.suggest(key.asString()));
        }

        return builder.buildFuture();
    }

    private int updateEquipability(@NotNull CommandContext<CommandSourceStack> context, final UnaryOperator<Equippable.Builder> function) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        final Equippable.Builder builder;
        if (stack.hasData(DataComponentTypes.EQUIPPABLE)) {
            builder = toBuilder(stack.getData(DataComponentTypes.EQUIPPABLE));
        } else {
            Utils.tell(context, "<red>Your item is <secondary>not equipable</secondary>! <gray>You should set that first!");
            Utils.tell(context, "<gray><hover:show_text:'<gray>This will suggest the command to set an item as equipable!'><click:suggest_command:'/ie equipable slot '>[Click me to suggest the command!]");
            return 0;
        }

        stack.setData(DataComponentTypes.EQUIPPABLE, function.apply(builder).build());
        return 1;
    }

    private Equippable.@NotNull Builder toBuilder(@NotNull Equippable equippable) {
        return toBuilder(equippable, equippable.slot());
    }

    private Equippable.@NotNull Builder toBuilder(@NotNull Equippable equippable, final EquipmentSlot slot) {
        return Equippable.equippable(slot)
                .allowedEntities(equippable.allowedEntities())
                .assetId(equippable.assetId())
                .cameraOverlay(equippable.cameraOverlay())
                .damageOnHurt(equippable.damageOnHurt())
                .dispensable(equippable.dispensable())
                .swappable(equippable.swappable());
    }
}
