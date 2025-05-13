package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.commands.arguments.GenericEnumArgument;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotDecorations;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PotSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie pot <secondary><side> <decoration></secondary>").hoverEvent(StringUtil.color("""
                <primary>Updates the rarity of the item.
                \s
                ◼ <secondary>[rarity]<required></secondary> The rarity to set the item to.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie pot "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("pot")
                .then(literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();

                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You must be holding a decorated pot!");
                                return 0;
                            }

                            if (stack.getType() != Material.DECORATED_POT) {
                                Utils.tell(context, "<red>You must be holding a decorated pot to change it's decorations.");
                                return 0;
                            }

                            stack.resetData(DataComponentTypes.POT_DECORATIONS);
                            Utils.tell(context, "<primary>Reset your decorated pot to default.");
                            return 1;
                        })
                )
                .then(argument("side", GenericEnumArgument.generic(Sides.class))
                        .then(argument("item", ArgumentTypes.resource(RegistryKey.ITEM))
                                .suggests((context, builder) -> {
                                    // While this doesn't DENY the use of other materials, this heavily incentivizes the use of just sherds and brick.
                                    // However, below we deny the use of any material other than sherds and a brick.
                                    Registry.ITEM.stream().forEach(type -> {
                                        final String material = Objects.requireNonNull(type.asMaterial()).key().asMinimalString().toLowerCase();
                                        if ((type.asMaterial() == Material.BRICK || material.contains("sherd")) && material.contains(builder.getRemainingLowerCase())) {
                                            builder.suggest(type.asMaterial().key().asString());
                                        }
                                    });
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();

                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        Utils.tell(user, "<red>You must be holding a decorated pot!");
                                        return 0;
                                    }

                                    if (stack.getType() != Material.DECORATED_POT) {
                                        Utils.tell(context, "<red>You must be holding a decorated pot to change it's decorations.");
                                        return 0;
                                    }

                                    final Sides side = context.getArgument("side", Sides.class);
                                    final ItemType type = context.getArgument("item", ItemType.class);
                                    // We ignore this deprecation because we require the use of the material, and there is no reason to build an ItemStack.
                                    final Material source = type.asMaterial();

                                    final PotDecorations.Builder decorations = stack.hasData(DataComponentTypes.POT_DECORATIONS) ? toBuilder(stack.getData(DataComponentTypes.POT_DECORATIONS)) : PotDecorations.potDecorations();

                                    if (source == Material.BRICK || source.key().asMinimalString().contains("sherd")) {
                                        switch (side) {
                                            case ALL -> decorations.front(type).back(type).left(type).right(type);
                                            case FRONT -> decorations.front(type);
                                            case BACK -> decorations.back(type);
                                            case LEFT -> decorations.left(type);
                                            case RIGHT -> decorations.right(type);
                                        }

                                        stack.setData(DataComponentTypes.POT_DECORATIONS, decorations.build());
                                        Utils.tell(context, "<primary>Set the decoration of your pot's <secondary>" + side.name().toLowerCase() + "</secondary> to <secondary>" + source.key().asMinimalString() + "</secondary>.");
                                        return 1;
                                    } else {
                                        Utils.tell(context, "<red>You must use a pottery sherd or a brick!");
                                        return 0;
                                    }
                                })
                        )
                );
    }

    private PotDecorations.@NotNull Builder toBuilder(@NotNull PotDecorations decorations) {
        return PotDecorations.potDecorations().back(decorations.back()).left(decorations.left()).right(decorations.right()).front(decorations.front());
    }

    private enum Sides {
        ALL,
        FRONT,
        BACK,
        LEFT,
        RIGHT
    }
}
