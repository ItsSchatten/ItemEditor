package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import org.bukkit.Location;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class CompassSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie compass <secondary><position></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set a compass' tracked location.
                \s
                ◼ <secondary><x><optional> <y><optional> <z><optional></secondary> Set the position the compass is tracking.
                ◼ <secondary>[-clear]<optional></secondary> Clear the tracked position.
                ◼ <secondary>[-view]<optional></secondary> View the tracked position.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie compass "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("compass")
                .then(literal("-clear")
                        .executes(context -> resetCompass((Player) context.getSource().getSender()))
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
                            if (!(stack.getItemMeta() instanceof final CompassMeta meta)) {
                                Utils.tell(user, "<red>You are not holding a compass!");
                                return 0;
                            }

                            if (meta.hasLodestone()) {
                                Utils.tell(user, "<primary>Your compass' location is currently: <secondary>" + StringUtil.prettifyLocation(Objects.requireNonNull(meta.getLodestone())) + "</secondary>!");
                            } else {
                                Utils.tell(user, "<primary>Your compass doesn't have a tracked location!");
                            }
                            return 1;
                        })
                )
                .then(argument("location", BlockPosArgument.blockPos())
                        .executes(context -> handleCompass((Player) context.getSource().getSender(), (meta) -> {
                            final BlockPos position = context.getArgument("location", Coordinates.class).getBlockPos((net.minecraft.commands.CommandSourceStack) context.getSource());
                            final Location location = new Location(context.getSource().getLocation().getWorld(), position.getX(), position.getY(), position.getZ());

                            meta.setLodestoneTracked(false);
                            meta.setLodestone(location);

                            Utils.tell(context.getSource(), "<primary>Updated your item's tracked location to <secondary>" + StringUtil.prettifyLocation(location) + "</secondary>!");
                            return meta;
                        }))
                )
                .executes(context -> handleCompass((Player) context.getSource().getSender(), (meta) -> {
                    meta.setLodestoneTracked(false);
                    meta.setLodestone(context.getSource().getLocation());

                    Utils.tell(context.getSource(), "<primary>Updated your item's tracked location to <secondary>" + StringUtil.prettifyLocation(context.getSource().getLocation()) + "</secondary>!");
                    return meta;
                }));
    }

    private int resetCompass(final @NotNull Player user) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof CompassMeta)) {
            Utils.tell(user, "<red>You are not holding a compass!");
            return 0;
        }

        // Temporary solution to actually reset a lodestone compass back to a normal compass.
        final net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(stack);
        nms.remove(DataComponents.LODESTONE_TRACKER);

        user.getInventory().setItemInMainHand(nms.asBukkitMirror());

        // Commented out as this does not work for Paper servers,
        // but leaving this here in the event the bug is patched.
        //meta.setLodestone(null);
        //meta.setLodestoneTracked(true);
        //stack.setItemMeta(meta);
        return 1;
    }

    private int handleCompass(final @NotNull Player user, final UnaryOperator<CompassMeta> function) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final CompassMeta meta)) {
            Utils.tell(user, "<red>You are not holding a compass!");
            return 0;
        }

        stack.setItemMeta(function.apply(meta));
        return 1;
    }
}
