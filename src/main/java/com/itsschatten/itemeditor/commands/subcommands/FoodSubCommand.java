package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.TimeUtils;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

// Added to suppress warnings about the unstable API for the FoodComponent.
// FIXME: Check if Food Component API is still considered unstable.
@SuppressWarnings({"UnstableApiUsage"})
public class FoodSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie food <secondary><food|saturation|eattime|effect|alwayseat|-clear|-view> <value></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Manipulate an item into being a food.
                        \s
                        ◼ <secondary><food><first></secondary> Set the nutritional value of this food.
                        ◼ <secondary><saturation><first></secondary> Set the saturation value of this food.
                        ◼ <secondary><eattime><first></secondary> Set how long it takes to eat this food in seconds.
                        ◼ <secondary><alwayseat><first></secondary> Set if the food can always be eaten.
                        ◼ <secondary><converts><first> <item><required|-clear></secondary> Set the item stack the food converts to once used.
                        ◼ <secondary><effects><first> <add|remove|-clear|-view><required></secondary> Manipulate food effects.
                        ◼ <secondary>[-view]<optional></secondary> View all the food values.
                        ◼ <secondary>[-clear]<optional></secondary> Removes all the food values.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie food "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("food")
                .then(Commands.literal("-view")
                        .executes(this::handleView)
                )
                .then(Commands.literal("-clear")
                        .executes(context -> updateFood(context, food -> {
                            Utils.tell(context.getSource(), "<primary>Your item is no longer considered food!");
                            return null;
                        }))
                )
                .then(Commands.literal("nutrition")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                .executes(context -> updateFood(context, food -> {
                                    final int nutrition = IntegerArgumentType.getInteger(context, "value");
                                    food.setNutrition(nutrition);

                                    // Required to ensure it doesn't throw an error when opening your inventory.
                                    if (food.getSaturation() == 0) {
                                        food.setSaturation(1);
                                    }

                                    if (food.getEatSeconds() == 0) {
                                        food.setEatSeconds(1);
                                    }
                                    return food;
                                }))
                        )
                )
                .then(Commands.literal("saturation")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(1.0f))
                                .executes(context -> updateFood(context, food -> {
                                    final float saturation = FloatArgumentType.getFloat(context, "value");
                                    food.setSaturation(saturation);

                                    // Required to ensure it doesn't throw an error when opening your inventory.
                                    if (food.getNutrition() == 0) {
                                        food.setNutrition(1);
                                    }

                                    if (food.getEatSeconds() == 0) {
                                        food.setEatSeconds(1);
                                    }
                                    return food;
                                }))
                        )
                )
                .then(Commands.literal("eattime")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(1.0f))
                                .executes(context -> updateFood(context, food -> {
                                    final float eatDuration = FloatArgumentType.getFloat(context, "value");
                                    food.setEatSeconds(eatDuration);

                                    // Required to ensure it doesn't throw an error when opening your inventory.
                                    if (food.getNutrition() == 0) {
                                        food.setNutrition(1);
                                    }

                                    if (food.getSaturation() == 0) {
                                        food.setSaturation(1);
                                    }
                                    return food;
                                }))
                        )
                )
                .then(Commands.literal("alwayseat")
                        .then(Commands.argument("value", BoolArgumentType.bool())
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
                )
                .then(Commands.literal("converts")
                        .then(Commands.literal("-clear")
                                .executes(context -> updateFood(context, food -> {
                                    food.setUsingConvertsTo(null);
                                    return food;
                                }))
                        )
                        .then(Commands.argument("item", ArgumentTypes.itemStack())
                                .executes(context -> updateFood(context, food -> {
                                    final ItemStack stack = context.getArgument("item", ItemStack.class);
                                    food.setUsingConvertsTo(stack);
                                    return food;
                                }))
                        )
                )
                .then(Commands.literal("effects")
                        .then(Commands.literal("add")
                                .then(Commands.argument("potion", ArgumentTypes.resource(RegistryKey.MOB_EFFECT))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("amplifier", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("probability", FloatArgumentType.floatArg(0.0f, 1.0f))
                                                                .executes(context -> updateFood(context, food -> {
                                                                    final float probability = FloatArgumentType.getFloat(context, "probability");
                                                                    final PotionEffect effect = buildEffect(context);
                                                                    food.addEffect(effect, probability);

                                                                    Utils.tell(context.getSource(), "<primary>Added the <secondary>" + effect.getType().key().asMinimalString() + " Duration (ticks): " + effect.getDuration() + " Amplification: " + effect.getAmplifier() +
                                                                            "</secondary> with a probability of <secondary>" + (probability * 100) + "%</secondary>");
                                                                    return food;
                                                                }))
                                                        )
                                                        .executes(context -> updateFood(context, food -> {
                                                            final PotionEffect effect = buildEffect(context);
                                                            food.addEffect(effect, 1f);

                                                            Utils.tell(context.getSource(), "<primary>Added the <secondary>" + effect.getType().key().asMinimalString() + " Duration (ticks): " + effect.getDuration() + " Amplification: " + effect.getAmplifier() +
                                                                    "</secondary> with a probability of <secondary>100%</secondary>");
                                                            return food;
                                                        }))
                                                )
                                        )
                                )
                        )

                        .then(Commands.literal("remove")
                                .then(Commands.argument("effect", IntegerArgumentType.integer(0))
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(getEffectIds(context).stream().map(integer -> integer + "").toList(), builder))
                                        .executes(context -> updateFood(context, food -> {
                                            final int effect = IntegerArgumentType.getInteger(context, "effect");
                                            final List<FoodComponent.FoodEffect> effects = food.getEffects();
                                            final FoodComponent.FoodEffect removed = effects.remove(effect);

                                            Utils.tell(context.getSource(), "<primary>Removed " + convertEffectToString(removed) + " from your food item.");

                                            food.setEffects(effects);
                                            return food;
                                        }))
                                )
                        )
                        .then(Commands.literal("-clear")
                                .executes(context -> updateFood(context, food -> {
                                    food.setEffects(Collections.emptyList());
                                    Utils.tell(context.getSource(), "<primary>Your food's effects have been cleared.");
                                    return food;
                                }))
                        )
                        .then(Commands.literal("-view")
                                .executes(this::handleEffectsView)
                        )
                )
                ;
    }

    private Collection<Integer> getEffectIds(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            return Collections.emptyList();
        }

        final ItemMeta meta = stack.getItemMeta();
        // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
        if (meta == null) {
            return Collections.emptyList();
        }

        return IntStream.rangeClosed(0, meta.getFood().getEffects().size() - 1).boxed().toList();
    }

    private @NotNull PotionEffect buildEffect(final @NotNull CommandContext<CommandSourceStack> context) {
        final PotionEffectType potion = context.getArgument("potion", PotionEffectType.class);

        final int duration = context.getArgument("duration", Integer.class);
        final int amplifier = context.getArgument("amplifier", Integer.class);

        return potion.createEffect(duration, amplifier);
    }

    private int updateFood(@NotNull CommandContext<CommandSourceStack> context, final Function<FoodComponent, FoodComponent> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
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
        if (stack.isEmpty()) {
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
        Utils.tell(user, "<primary>◼ Consumed In (seconds): <secondary>" + food.getEatSeconds() + "</secondary>");
        Utils.tell(user, "<primary>◼ Can Always Eat Value: <secondary>" + food.canAlwaysEat() + "</secondary>");
        Utils.tell(user, StringUtil.color("<primary>◼ Can Always Eat Value: ")
                .append(StringUtil.color("<secondary>" + (food.getUsingConvertsTo() != null ? food.getUsingConvertsTo().getType() : "nothing") + "</secondary>")
                        .hoverEvent((food.getUsingConvertsTo() != null ? food.getUsingConvertsTo().asHoverEvent() : StringUtil.color("<gray>;)")))));
        Utils.tell(user, "<primary>◼ Effects: <secondary>" + food.getEffects().size() + "</secondary> " +
                "<gray>(Use <hover:show_text:'<primary>Click me to suggest the command!'><click:suggest_command:'/ie food effects -view'>/ie food effects -view</click></hover>) to view them.)");

        return 1;
    }

    private int handleEffectsView(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        final ItemMeta meta = stack.getItemMeta();
        // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        Utils.tell(user, StringUtil.color("<gradient:#D8D8F6:#978897>Your Food's Effects:</gradient>"));
        meta.getFood().getEffects().forEach(effect -> Utils.tell(user, "<primary>◼</primary> " + convertEffectToString(effect)));
        return 1;
    }

    // Utility method to convert a FoodEffect to a human-readable string.
    private @NotNull String convertEffectToString(final FoodComponent.@NotNull FoodEffect effect) {
        final PotionEffect potionEffect = effect.getEffect();

        return "<secondary>" + (effect.getProbability() * 100) + "% <gray>|</gray> " +
                potionEffect.getType().key().asMinimalString() +
                " " + TimeUtils.getMinecraftTimeAsString(potionEffect.getDuration()) +
                " " + StringUtil.convertToRomanNumeral(potionEffect.getAmplifier()) + "</secondary>";
    }
}
