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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public final class GenericEnumArgument<E extends Enum<E>> implements CustomArgumentType.Converted<E, String> {

    final Class<E> enumClass;

    private GenericEnumArgument(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Contract(value = "_ -> new", pure = true)
    public static <T extends Enum<T>> @NotNull GenericEnumArgument<T> generic(final Class<T> enumClass) {
        return new GenericEnumArgument<>(enumClass);
    }

    @Override
    public @NotNull <S> CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(EnumSet.allOf(enumClass).stream()
                .map((operation) -> operation.name().toLowerCase())
                .toList(), builder);
    }

    @Override
    public @NotNull E convert(@NotNull String nativeType) throws CommandSyntaxException {
        try {
            return Enum.valueOf(enumClass, nativeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(Component.literal("Failed to find '" + nativeType + "' in " + enumClass.getSimpleName() + ".")).create();
        } catch (NullPointerException e) {
            throw new SimpleCommandExceptionType(Component.literal("Enum class is null while attempting to parse a value for it!")).create();
        }
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
