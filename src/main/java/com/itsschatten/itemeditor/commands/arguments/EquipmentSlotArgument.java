package com.itsschatten.itemeditor.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public final class EquipmentSlotArgument implements CustomArgumentType.Converted<EquipmentSlotGroup, String> {
    private final List<String> VALUES = List.of("any", "armor", "body", "chest", "feet", "hand", "head", "legs", "mainhand", "offhand");

    @Override
    public @NotNull EquipmentSlotGroup convert(@NotNull String nativeType) throws CommandSyntaxException {
        try {
            return Objects.requireNonNull(EquipmentSlotGroup.getByName(nativeType));
        } catch (NullPointerException e) {
            throw new SimpleCommandExceptionType(Component.literal("Couldn't find an equipment slot with the name '" + nativeType + "'.")).create();
        }
    }

    @Override
    public @NotNull <S> CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(VALUES, builder);
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
