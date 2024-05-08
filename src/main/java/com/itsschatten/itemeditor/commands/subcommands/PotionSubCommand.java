package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.StringWrapUtils;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class PotionSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public PotionSubCommand(@NotNull CommandBase owningCommand) {
        super("potion", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><add|base|remove|-clear|-view> ...").hoverEvent(StringUtil.color("""
                <primary>Add or remove an attribute from an item.
                \s
                ◼ <secondary><base><required> <type><required></secondary> Set the base potion type.
                ◼ <secondary><add><required> <effect><required> <duration><required> [level]<optional></secondary> Add a potion effect to the potion.
                ◼ <secondary><remove><required> <effect><required></secondary> Remove a potion effect from the potion.
                ◼ <secondary><-clear><required></secondary> Clear all effects on the potion.
                ◼ <secondary><-view><optional></secondary> View all potion effects on the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        if (!(stack.getItemMeta() instanceof final PotionMeta meta)) {
            returnTell("<red>For some reason the item's meta is null!");
            return;
        }

        if (args.length == 0) {
            returnTell("<red>Please provide add, remove, -clear, or -view.");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "base" -> {
                // We need an argument.
                if (args.length == 1) {
                    tell("<red>Provide the base potion type you wish to set your potion to.");
                    return;
                }

                try {
                    // Get the potion type from the registry.
                    final NamespacedKey enchantmentKey = args[1].contains(":") ? NamespacedKey.fromString(args[1]) : NamespacedKey.minecraft(args[1]);
                    if (enchantmentKey == null) {
                        returnTell("<red>Could not find a potion type by the name <yellow>" + args[1] + "</yellow>.");
                        return;
                    }

                    final PotionType potionType = Registry.POTION.get(enchantmentKey);
                    if (potionType == null) {
                        returnTell("<red>Could not find a potion type by the name <yellow>" + args[1] + "</yellow>.");
                        return;
                    }

                    // Set base type.
                    meta.setBasePotionType(potionType);
                    tell("<primary>Set your potion's base type to <secondary>" + potionType.key().asString() + "</secondary>.");
                } catch (IllegalArgumentException ex) {
                    tell("<yellow>" + args[1] + "<red> is not a valid potion type.");
                    return;
                }
            }

            case "add" -> {
                // Storage map for the effect.
                final Map<String, Object> map = new HashMap<>();

                if (args.length == 1) {
                    tell("<red>Provide a potion effect to add to your potion!");
                    return;
                }

                // Get the potion type from the registry.
                final NamespacedKey enchantmentKey = args[1].contains(":") ? NamespacedKey.fromString(args[1]) : NamespacedKey.minecraft(args[1]);
                if (enchantmentKey == null) {
                    returnTell("<red>Could not find a potion effect type by the name <yellow>" + args[1] + "</yellow>.");
                    return;
                }

                final PotionEffectType potionEffectType = Registry.POTION_EFFECT_TYPE.get(enchantmentKey);
                if (potionEffectType == null) {
                    returnTell("<red>Could not find a potion effect type by the name <yellow>" + args[1] + "</yellow>.");
                    return;
                }

                // Make sure we have a duration.
                if (args.length == 2) {
                    returnTell("<red>Please provide the duration you want " + potionEffectType.key().asString() + " to be!");
                    return;
                }

                // Get the duration, a required argument.
                final int duration = getNumber(2, "<yellow>" + args[2] + "<red> is not a valid integer.");
                // The amplifier, default to one if not provided.
                final int amplifier = args.length == 3 ? 1 : getNumber(3, "<red>" + args[3] + "<red> is not a valid integer.");

                // Add all the things to the map.
                map.put("effect", potionEffectType.getKey().asString());
                map.put("duration", duration <= 0 ? PotionEffect.INFINITE_DURATION : duration);
                map.put("amplifier", amplifier);

                // Make the effect and add the custom effect.
                final PotionEffect effect = new PotionEffect(map);
                meta.addCustomEffect(effect, true);
                tell("<primary>Added <secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>" + effect.getType().key().asString() + "</hover></secondary> to your potion!");
            }

            case "-clear" -> {
                meta.setBasePotionType(null);
                meta.clearCustomEffects();
                tell("<primary>Cleared all potion effects from your potion, this includes the base type.");
            }

            case "remove" -> {
                // We need args.
                if (args.length == 1) {
                    tell("<red>Provide a potion effect to remove from your potion!");
                    return;
                }

                // Get the potion type from the registry.
                final NamespacedKey enchantmentKey = args[1].contains(":") ? NamespacedKey.fromString(args[1]) : NamespacedKey.minecraft(args[1]);
                if (enchantmentKey == null) {
                    returnTell("<red>Could not find a potion effect type by the name <yellow>" + args[1] + "</yellow>.");
                    return;
                }

                final PotionEffectType potionEffectType = Registry.POTION_EFFECT_TYPE.get(enchantmentKey);
                if (potionEffectType == null) {
                    returnTell("<red>Could not find a potion effect type by the name <yellow>" + args[1] + "</yellow>.");
                    return;
                }

                meta.removeCustomEffect(potionEffectType);
                tell("<primary>Removed <secondary>" + potionEffectType.key().asString() + "</secondary> from your potion!");
            }

            case "-view" -> {
                if (meta.hasBasePotionType()) {
                    tell("<primary>Your potion doesn't have a base type.");
                } else {
                    tell("<primary>Your base potion type is: <secondary>" + meta.getBasePotionType().key().asString() + "</secondary>.");
                }

                if (meta.hasCustomEffects()) {
                    tell("<primary>Your potion has no custom effects.");
                } else {
                    final List<String> wrapString = new ArrayList<>();
                    meta.getCustomEffects().forEach((effect) -> {
                        wrapString.add("<secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>" + effect.getType().key().asString() + "</hover></secondary>");
                    });

                    final String toWrap = String.join(", ", wrapString);

                    tell("<primary>Your potion has the following custom effects:" + StringWrapUtils.wrap(toWrap, 35, "|"));
                }

                return;
            }

            default -> returnTell("<red>Please provide add, remove, -clear, or -view.");
        }

        stack.setItemMeta(meta);
    }

    private @NotNull String getHoverFromEffect(final @NotNull PotionEffect effect) {
        return """
                <primary>Duration: <secondary>{duration}</secondary>
                Amplifier: <secondary>{amplifier}</secondary>"""
                .replace("{duration}", effect.getDuration() + "")
                .replace("{amplifier}", effect.getAmplifier() + "")
                ;
    }


    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("add", "remove", "base", "-clear", "-view").filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("base")) {
                    return Registry.POTION.stream().map((effect) -> effect.key().asString()).filter((name) -> name.contains(args[1].toLowerCase(Locale.ROOT))).toList();
                }

                if (args[0].equalsIgnoreCase("remove") && sender instanceof Player player && player.getInventory().getItemInMainHand().getItemMeta() instanceof PotionMeta meta) {
                    return meta.hasCustomEffects() ? super.getTabComplete(sender, args) :
                            meta.getCustomEffects().stream().map((effect) -> effect.getType().key().asString()).filter((name) -> name.contains(args[1].toLowerCase(Locale.ROOT))).toList();
                }

                if (args[0].equalsIgnoreCase("add")) {
                    return Registry.POTION_EFFECT_TYPE.stream().map((effect) -> effect.key().asString()).filter((name) -> name.contains(args[1].toLowerCase(Locale.ROOT))).toList();
                }
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
                return Stream.of("-1", "120", "60").filter((name) -> name.contains(args[2].toLowerCase(Locale.ROOT))).toList();
            }

            if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
                return Stream.of("1", "10").filter((name) -> name.contains(args[3].toLowerCase(Locale.ROOT))).toList();
            }
        }
        return super.getTabComplete(sender, args);
    }
}
