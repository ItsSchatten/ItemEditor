package com.itsschatten.itemeditor.utils;

import lombok.Builder;
import lombok.Singular;
import net.kyori.adventure.key.Key;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Does nothing on its own.
 */
public interface ConsumeEffectOptions {

    @Builder(toBuilder = true)
    record ApplyStatusEffectsOptions(@Singular List<PotionEffect> effects, float probability) implements ConsumeEffectOptions {
    }

    record ClearAllStatusEffectsOptions() implements ConsumeEffectOptions {
    }

    @Builder(toBuilder = true)
    record PlaySoundOptions(Key sound) implements ConsumeEffectOptions {
    }

    @Builder
    record RandomTeleportOptions(float diameter) implements ConsumeEffectOptions {
    }

    @Builder(toBuilder = true)
    record RemoveStatusEffectsOptions(@Singular List<PotionEffectType> effects) implements ConsumeEffectOptions {
    }

}
