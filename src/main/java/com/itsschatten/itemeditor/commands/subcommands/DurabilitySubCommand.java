package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public final class DurabilitySubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie durability <secondary><repair|add|set|remove|damage #></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets a damageable item's damage or it's maximum damage.
                \s
                ◼ <secondary><damage #><required></secondary> The damage value.
                ◼ <secondary><repair><first></secondary> Repair the item.
                ◼ <secondary><add><first></secondary> Add the damage to the item.
                ◼ <secondary><set><first></secondary> Set the damage of the item.
                ◼ <secondary><remove><first></secondary> Remove the damage from the item.
                ◼ <secondary><max><first> <maximum damage><required></secondary> Remove the damage from the item.
                ◼ <secondary>[-view]<optional></secondary> See the current damage.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie durability "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("durability")
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
                            if (!(stack.getItemMeta() instanceof final Damageable meta)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (meta.hasMaxDamage()) {
                                Utils.tell(user, "<primary>Your item's damage is currently <secondary>" + meta.getDamage() + "</secondary><gray>/" + meta.getMaxDamage() + " (<hover:show_text:'<primary>Default: <secondary>" + stack.getType().getMaxDurability() + "</secondary>'><i>custom max damage, hover for default.</i></hover>)</gray>.");
                            } else {
                                Utils.tell(user, "<primary>Your item's damage is currently <secondary>" + meta.getDamage() + "</secondary><gray>/" + stack.getType().getMaxDurability() + "</gray>.");
                            }

                            return 1;
                        })
                )
                .then(literal("repair")
                        .executes(context -> updateDurability((Player) context.getSource().getSender(), (meta, defaultMax) -> {
                            meta.setDamage(0);
                            Utils.tell(context.getSource(), "<primary>Repaired your item!<br>Set the damage of your item to <secondary>0</secondary><gray>/" + (meta.hasMaxDamage() ? meta.getMaxDamage() : defaultMax) + "</gray>.");
                            return meta;
                        }))
                )
                .then(literal("max")
                        .then(argument("maximum durability", IntegerArgumentType.integer(1))
                                .then(literal("-reset")
                                        .executes(context -> updateDurability((Player) context.getSource().getSender(), (meta, defaultMax) -> {
                                            meta.resetDamage();
                                            Utils.tell(context.getSource(), "<primary>Reset your item's maximum durability to <secondary>" + defaultMax + "</secondary>!");
                                            return meta;
                                        }))
                                )
                                .executes(context -> updateDurability((Player) context.getSource().getSender(), (meta, defaultMax) -> {
                                    final int amount = IntegerArgumentType.getInteger(context, "maximum durability");

                                    meta.setMaxDamage(amount);
                                    Utils.tell(context.getSource(), "<primary>Updated your item's max damage to <secondary>" + amount + "</secondary>!");
                                    return meta;
                                }))
                        )
                )
                .then(argument("durability", IntegerArgumentType.integer(0))
                        .executes(context -> updateDurability((Player) context.getSource().getSender(), (meta, defaultMax) -> {
                            final int amount = IntegerArgumentType.getInteger(context, "durability");

                            meta.setDamage(amount);
                            Utils.tell(context.getSource(), "<primary>Set the damage of your item to <secondary>" + amount + "</secondary><gray>/" + (meta.hasMaxDamage() ? meta.getMaxDamage() : defaultMax) + "</gray>.");
                            return meta;
                        }))
                )
                .then(literal("set")
                        .then(argument("durability", IntegerArgumentType.integer(0))
                                .executes(context -> updateDurability((Player) context.getSource().getSender(), (meta, defaultMax) -> {
                                    final int amount = IntegerArgumentType.getInteger(context, "durability");

                                    meta.setDamage(amount);
                                    Utils.tell(context.getSource(), "<primary>Set the damage of your item to <secondary>" + amount + "</secondary><gray>/" + (meta.hasMaxDamage() ? meta.getMaxDamage() : defaultMax) + "</gray>.");
                                    return meta;
                                }))
                        )
                )
                .then(literal("remove")
                        .then(argument("durability", IntegerArgumentType.integer(0))
                                .executes(context -> updateDurability((Player) context.getSource().getSender(), (meta, defaultMax) -> {
                                    final int amount = IntegerArgumentType.getInteger(context, "durability");

                                    meta.setDamage(meta.getDamage() - amount);
                                    Utils.tell(context.getSource(), "<primary>Removed <secondary>" + amount + "</secondary> damage from your item. <gray>(" + meta.getDamage() + "/" + (meta.hasMaxDamage() ? meta.getMaxDamage() : defaultMax) + ")");
                                    return meta;
                                }))
                        )
                )
                .then(literal("add")
                        .then(argument("durability", IntegerArgumentType.integer(0))
                                .executes(context -> updateDurability((Player) context.getSource().getSender(), (meta, defaultMax) -> {
                                    final int amount = IntegerArgumentType.getInteger(context, "durability");

                                    meta.setDamage(amount + meta.getDamage());
                                    Utils.tell(context.getSource(), "<primary>Added <secondary>" + amount + "</secondary> damage to your item. <gray>(" + meta.getDamage() + "/" + (meta.hasMaxDamage() ? meta.getMaxDamage() : defaultMax) + ")");
                                    return meta;
                                }))
                        )
                )
                ;
    }

    private int updateDurability(final @NotNull Player user, final BiFunction<Damageable, Short, Damageable> function) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final Damageable meta)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.setItemMeta(function.apply(meta, stack.getType().getMaxDurability()));
        return 1;
    }
}
