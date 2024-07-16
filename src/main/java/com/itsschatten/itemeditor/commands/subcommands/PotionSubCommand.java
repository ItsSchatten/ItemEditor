package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.StringWrapUtils;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

public class PotionSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie potion <secondary><add|base|remove|-clear|-view> ...").hoverEvent(StringUtil.color("""
                <primary>Add or remove an attribute from an item.
                \s
                ◼ <secondary><base><required> <type><required></secondary> Set the base potion type.
                ◼ <secondary><add><required> <effect><required> <duration><required> [level]<optional></secondary> Add a potion effect to the potion.
                ◼ <secondary><remove><required> <effect><required></secondary> Remove a potion effect from the potion.
                ◼ <secondary><-clear><required></secondary> Clear all effects on the potion.
                ◼ <secondary><-view><optional></secondary> View all potion effects on the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie potion "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("potion")
                .then(Commands.literal("base")
                        .then(Commands.literal("-clear")
                                .executes(context -> updatePotion(context, potion -> {
                                    potion.setBasePotionType(null);

                                    Utils.tell(context.getSource(), "<primary>Cleared your potions base effect." +
                                            "<gray><i><hover:show_text:'<gray>Click to suggest the command.'><click:suggest_command:'/ie potion -clear'>To remove custom effects run '/ie potion -clear'</click></hover>"
                                    );
                                    return potion;
                                }))
                        )
                        .then(Commands.argument("potion", ArgumentTypes.resource(RegistryKey.POTION))
                                .executes(context -> updatePotion(context, potion -> {
                                    final PotionType type = context.getArgument("potion", PotionType.class);
                                    potion.setBasePotionType(type);

                                    Utils.tell(context.getSource(), "<primary>Set your potion's base type to <secondary>" + type.key().asString() + "</secondary>.");
                                    return potion;
                                }))
                        )
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("potion", ArgumentTypes.resource(RegistryKey.MOB_EFFECT))
                                .then(Commands.argument("duration", IntegerArgumentType.integer(-1))
                                        .then(Commands.argument("amplifier", IntegerArgumentType.integer(1))
                                                .executes(context -> updatePotion(context, potion -> {
                                                    final PotionEffect effect = buildEffect(context);
                                                    potion.addCustomEffect(effect, true);

                                                    Utils.tell(context.getSource(), "<primary>Added <secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>" + effect.getType().key().asString() + "</hover></secondary> to your potion!");
                                                    return potion;
                                                }))
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("potion", ArgumentTypes.resource(RegistryKey.MOB_EFFECT))
                                .executes(context -> updatePotion(context, potion -> {
                                    final PotionEffectType type = context.getArgument("potion", PotionEffectType.class);
                                    potion.removeCustomEffect(type);

                                    Utils.tell(context.getSource(), "<primary>Removed <secondary>" + type.key().asString() + "</secondary> from your potion!");
                                    return potion;
                                }))
                        )
                )
                .then(Commands.literal("-clear")
                        .executes(context -> updatePotion(context, potion -> {
                            potion.clearCustomEffects();
                            Utils.tell(context.getSource(), "<primary>Cleared all custom potion effects from your potion." +
                                    "\n<gray><i><hover:show_text:'<gray>Click to suggest the command.'><click:suggest_command:'/ie potion base -clear'>To remove the base effect run '/ie potion base -clear'</click></hover>");
                            return potion;
                        }))
                )
                .then(Commands.literal("-view")
                        .executes(this::handleView)
                )
                ;
    }

    private int handleView(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final PotionMeta meta)) {
            Utils.tell(user, "<red>The item you are holding is not a potion!");
            return 0;
        }

        if (meta.hasBasePotionType()) {
            Utils.tell(context.getSource(), "<primary>Your potion doesn't have a base type.");
        } else {
            Utils.tell(context.getSource(), "<primary>Your base potion type is: <secondary>" + meta.getBasePotionType().key().asString() + "</secondary>.");
        }

        if (meta.hasCustomEffects()) {
            Utils.tell(context.getSource(), "<primary>Your potion has no custom effects.");
        } else {
            final List<String> wrapString = new ArrayList<>();
            meta.getCustomEffects().forEach((effect) -> wrapString.add("<secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>" + effect.getType().key().asString() + "</hover></secondary>"));

            final String toWrap = String.join(", ", wrapString);
            Utils.tell(context.getSource(), "<primary>Your potion has the following custom effects:" + StringWrapUtils.wrap(toWrap, 35, "|"));
        }

        return 1;
    }

    private int updatePotion(final @NotNull CommandContext<CommandSourceStack> context, final Function<PotionMeta, PotionMeta> function) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final PotionMeta meta)) {
            Utils.tell(user, "<red>The item you are holding is not a potion!");
            return 0;
        }

        stack.setItemMeta(function.apply(meta));

        return 1;
    }

    private @NotNull PotionEffect buildEffect(final @NotNull CommandContext<CommandSourceStack> context) {
        final PotionEffectType potion = context.getArgument("potion", PotionEffectType.class);

        final int duration = context.getArgument("duration", Integer.class);
        final int amplifier = context.getArgument("amplifier", Integer.class);

        return potion.createEffect(duration <= 0 ? PotionEffect.INFINITE_DURATION : duration, amplifier);
    }

    private @NotNull String getHoverFromEffect(final @NotNull PotionEffect effect) {
        return """
                <primary>Duration: <secondary>{duration}</secondary>
                Amplifier: <secondary>{amplifier}</secondary>"""
                .replace("{duration}", effect.getDuration() + "")
                .replace("{amplifier}", effect.getAmplifier() + "")
                ;
    }
}
