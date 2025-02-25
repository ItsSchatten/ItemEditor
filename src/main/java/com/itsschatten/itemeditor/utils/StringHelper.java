package com.itsschatten.itemeditor.utils;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.TimeUtils;
import lombok.experimental.UtilityClass;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@UtilityClass
public final class StringHelper {

    public @NotNull String potionEffectToString(final @NotNull PotionEffect effect) {
        return effect.getType().key().asMinimalString() +
                "<gold>" + StringUtil.convertToRomanNumeral(effect.getAmplifier(), true, true) + "</gold>" +
                " <dark_gray>[" + (effect.isInfinite() ? "∞" : TimeUtils.getMinecraftTimeAsStringShort(effect.getDuration())) + "]</dark_gray>" +
                " " + (effect.hasParticles() ? "<green>✔</green>" : "<red>✘</red>") +
                (effect.hasIcon() ? "<green>✔</green>" : "<red>✘</red>") +
                (effect.isAmbient() ? "<green>✔</green>" : "<red>✘</red>");
    }

    public @NotNull String firstEffect(@NotNull List<PotionEffect> effects) {
        final StringBuilder builder = new StringBuilder();

        if (effects.isEmpty()) {
            return "<red>Empty";
        }

        final PotionEffect effect = effects.getFirst();
        builder.append(StringHelper.potionEffectToString(effect));
        effects.remove(effect);

        return builder.toString();
    }


    public @NotNull String firstTwoEffectTypes(@NotNull List<PotionEffectType> effects) {
        final StringBuilder builder = new StringBuilder();

        if (effects.isEmpty()) {
            return "<red>Empty";
        }

        final PotionEffectType effect = effects.getFirst();
        builder.append(effect.key().asMinimalString());
        if (effects.size() >= 2) {
            final PotionEffectType secondEffect = effects.get(1);
            builder.append("<gray>, </gray>").append(secondEffect.key().asMinimalString());

            effects.remove(secondEffect);
        }

        effects.remove(effect);
        return builder.toString();
    }

    public String conditionString(final String string, final String other, final @NotNull Predicate<String> predicate) {
        return predicate.test(string) ? string : other;
    }

}
