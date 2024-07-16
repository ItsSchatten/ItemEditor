package com.itsschatten.itemeditor.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.minecraft.network.chat.Component;
import org.bukkit.Color;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ColorArgument implements CustomArgumentType.Converted<Color, String> {

    @Override
    public @NotNull Color convert(@NotNull String nativeType) throws CommandSyntaxException {
        if (nativeType.contains("\"")) {
            nativeType = nativeType.replace("\"", "");
        }

        if (nativeType.contains("#")) {
            nativeType = nativeType.replace("#", "");
        }

        if (nativeType.length() < 6) {
            throw new SimpleCommandExceptionType(Component.literal("A hex color string must contain 6 characters (not including a #)")).create();
        }

        return Color.fromRGB(Integer.parseInt(nativeType.substring(0, 6), 16));
    }


    @Contract(value = " -> new", pure = true)
    public static @NotNull ColorArgument color() {
        return new ColorArgument();
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }
}
