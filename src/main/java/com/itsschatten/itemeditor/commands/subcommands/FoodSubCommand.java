package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.item.SuspiciousStewEffects;
import io.papermc.paper.potion.SuspiciousEffectEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

// Added to suppress warnings about the unstable API for the FoodComponent.
public final class FoodSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie food <secondary><food|saturation|eattime|effect|alwayseat|-clear|-view> <value></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Manipulate an item into being a food.
                        \s
                        ◼ <secondary><food><first></secondary> Set the nutritional value of this food.
                        ◼ <secondary><saturation><first></secondary> Set the saturation value of this food.
                        ◼ <secondary><alwayseat><first></secondary> Set if the food can always be eaten.
                        ◼ <secondary>[-view]<optional></secondary> View all the food values.
                        ◼ <secondary>[-clear]<optional></secondary> Removes all the food values.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie food "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("food")
                .then(literal("-view")
                        .executes(this::handleView)
                )
                .then(literal("-clear")
                        .executes(context -> updateFood(context, food -> {
                            Utils.tell(context.getSource(), "<primary>Your item is no longer considered food!");
                            return null;
                        }))
                )
                .then(literal("nutrition")
                        .then(argument("value", IntegerArgumentType.integer(1))
                                .executes(context -> updateFood(context, food -> {
                                    final int nutrition = IntegerArgumentType.getInteger(context, "value");
                                    food.setNutrition(nutrition);

                                    // Required to ensure it doesn't throw an error when opening your inventory.
                                    if (food.getSaturation() == 0) {
                                        food.setSaturation(1);
                                    }

                                    final float bars = nutrition / 2f;
                                    Utils.tell(context, "<primary>Your item will now nourish the consumer by <secondary>" + nutrition + "</secondary> <gray>(" + bars + " " + (bars > 1f ? "bars" : "bar") + ")</gray>!");
                                    return food;
                                }))
                        )
                )
                .then(literal("saturation")
                        .then(argument("value", FloatArgumentType.floatArg(1.0f))
                                .executes(context -> updateFood(context, food -> {
                                    final float saturation = FloatArgumentType.getFloat(context, "value");
                                    food.setSaturation(saturation);

                                    // Required to ensure it doesn't throw an error when opening your inventory.
                                    if (food.getNutrition() == 0) {
                                        food.setNutrition(1);
                                    }

                                    Utils.tell(context, "<primary>Your item will now saturate the consumer by <secondary>" + saturation + "</secondary>!");
                                    return food;
                                }))
                        )
                )
                .then(literal("alwayseat")
                        .then(argument("value", BoolArgumentType.bool())
                                .executes(context -> updateFood(context, food -> {
                                    final boolean value = BoolArgumentType.getBool(context, "value");

                                    food.setCanAlwaysEat(value);
                                    Utils.tell(context.getSource(), "<primary>Your food <secondary>" + (food.canAlwaysEat() ? "can" : "can not") + "</secondary> always be eaten.");
                                    return food;
                                }))
                        )
                        .executes(context -> updateFood(context, food -> {
                            food.setCanAlwaysEat(!food.canAlwaysEat());
                            Utils.tell(context.getSource(), "<primary>Your food <secondary>" + (food.canAlwaysEat() ? "can" : "can not") + "</secondary> always be eaten.");
                            return food;
                        }))
                );
    }

    private int updateFood(@NotNull CommandContext<CommandSourceStack> context, final UnaryOperator<FoodComponent> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        final ItemMeta meta = stack.getItemMeta();
        // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        final FoodComponent food = function.apply(meta.getFood());
        meta.setFood(food);
        stack.setItemMeta(meta);
        return 1;
    }

    private int handleView(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        final ItemMeta meta = stack.getItemMeta();
        // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }


        final FoodComponent food = meta.getFood();
        Utils.tell(user, StringUtil.color("<gradient:#D8D8F6:#978897>Your Food's Information:</gradient>"));
        Utils.tell(user, "<primary>◼ Nutritional Value: <secondary>" + food.getNutrition() + "</secondary>");
        Utils.tell(user, "<primary>◼ Saturation Value: <secondary>" + food.getSaturation() + "</secondary>");
        Utils.tell(user, "<primary>◼ Can Always Eat Value: <secondary>" + food.canAlwaysEat() + "</secondary>");
        return 1;
    }
}
