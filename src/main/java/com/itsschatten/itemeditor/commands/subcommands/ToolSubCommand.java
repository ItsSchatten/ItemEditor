package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public final class ToolSubCommand extends BrigadierCommand {
    private static final DynamicCommandExceptionType ERROR_INVALID_BLOCK = new DynamicCommandExceptionType(id -> net.minecraft.network.chat.Component.literal("Failed to find a valid block or tag for: " + id));

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie tool <function> <secondary><block|blocks|block tag> [correct for drops|speed] [correct for drops]</secondary>")
                .hoverEvent(StringUtil.color("""
                        <primary>Manipulates tool information for your item.
                        \s
                        ◼ <secondary><add><required> <block|blocks|block tag><required> [speed|correct for drops]<optional> [correct for drop]<optional></secondary> Add a tool rule to your item.
                        ◼ <secondary><remove><required> <rule|-all|last><required></secondary> Remove a tool rule from your item.
                        ◼ <secondary><speed><required> <value <info>float</info>><required></secondary> Updates the default mining speed for the item.
                        ◼ <secondary><damage><required> <value <info>integer</info>><required></secondary> Updates the damage the item takes per block broken.
                        ◼ <secondary><-view><optional></secondary> View tool information for your item.
                        ◼ <secondary><-clear><required></secondary> Clear tool information from your item.
                        """).asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie tool "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("tool")
                .then(literal("add")
                        .then(argument("block or tag", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.BLOCK))
                                .then(argument("speed", FloatArgumentType.floatArg(0.0f))
                                        .then(argument("correct for drops", BoolArgumentType.bool())
                                                .executes(context -> addRuleResourceOrTag(context, FloatArgumentType.getFloat(context, "speed"), BoolArgumentType.getBool(context, "correct for drops")))
                                        )
                                        .executes(context -> addRuleResourceOrTag(context, FloatArgumentType.getFloat(context, "speed"), false))
                                )
                                .then(argument("correct for drops", BoolArgumentType.bool())
                                        .executes(context -> addRuleResourceOrTag(context, 0, BoolArgumentType.getBool(context, "correct for drops")))
                                )
                                .executes(context -> addRuleResourceOrTag(context, 0, true))
                        )

                        .then(argument("blocks", StringArgumentType.string())
                                .then(argument("speed", FloatArgumentType.floatArg(0.0f))
                                        .then(argument("correct for drops", BoolArgumentType.bool())
                                                .executes(context -> addRuleBlocks(context, FloatArgumentType.getFloat(context, "speed"), BoolArgumentType.getBool(context, "correct for drops")))
                                        )
                                        .executes(context -> addRuleBlocks(context, FloatArgumentType.getFloat(context, "speed"), false))
                                )
                                .then(argument("correct for drops", BoolArgumentType.bool())
                                        .executes(context -> addRuleBlocks(context, 0, BoolArgumentType.getBool(context, "correct for drops")))
                                )
                                .executes(context -> addRuleBlocks(context, 0, false))
                        )
                )
                .then(literal("remove")
                        .then(literal("-all")
                                .executes(context -> updateTool(context, tool -> {
                                    tool.setRules(List.of());
                                    Utils.tell(context.getSource(), "<primary>Removed <secondary>all</secondary> tool rules from your item.");
                                    return tool;
                                }))
                        )
                        .then(literal("last")
                                .executes(context -> updateTool(context, tool -> {
                                    final int number = tool.getRules().size() - 1;
                                    final ToolComponent.ToolRule rule = tool.getRules().getLast();
                                    tool.removeRule(rule);

                                    Utils.tell(context.getSource(), "<primary>Removed tool <secondary><hover:show_text:'" + convertToHover(rule) + "'><click:suggest_command:'" + convertToClick(rule) + "'>#" + number + "</click></hover></secondary> from your item!");
                                    Utils.tell(context.getSource(), "<info>Hover over the number to see the rule you've removed, click it to suggest the command to re-add it.");
                                    return tool;
                                }))
                        )
                        .then(argument("rule", IntegerArgumentType.integer(0))
                                .suggests((context, builder) -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        return builder.buildFuture();
                                    }

                                    final ItemMeta meta = stack.getItemMeta();
                                    // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
                                    if (meta == null) {
                                        return builder.buildFuture();
                                    }

                                    return SharedSuggestionProvider.suggest(getRuleAmount(meta), builder);
                                })
                                .executes(context -> updateTool(context, tool -> {
                                    final int number = IntegerArgumentType.getInteger(context, "rule");
                                    final ToolComponent.ToolRule rule = tool.getRules().get(number - 1);
                                    tool.removeRule(rule);

                                    Utils.tell(context.getSource(), "<primary>Removed tool <secondary><hover:show_text:'" + convertToHover(rule) + "'><click:suggest_command:'" + convertToClick(rule) + "'>#" + number + "</click></hover></secondary> from your item!");
                                    Utils.tell(context.getSource(), "<info>Hover over the number to see the rule you've removed, click it to suggest the command to re-add it.");
                                    return tool;
                                }))
                        )
                )
                .then(literal("speed")
                        .then(argument("speed", FloatArgumentType.floatArg(0.0F))
                                .executes(context -> updateTool(context, tool -> {
                                    final float speed = FloatArgumentType.getFloat(context, "speed");
                                    tool.setDefaultMiningSpeed(speed);
                                    Utils.tell(context.getSource(), "<primary>Your item's default mining speed is now <secondary>" + speed + "</secondary>.");
                                    return tool;
                                }))
                        )
                )
                .then(literal("damage")
                        .then(argument("damage per block", IntegerArgumentType.integer())
                                .executes(context -> updateTool(context, tool -> {
                                    final int dmg = IntegerArgumentType.getInteger(context, "damage per block");
                                    tool.setDamagePerBlock(dmg);
                                    Utils.tell(context.getSource(), "<primary>Your item now takes <secondary>" + dmg + "</secondary> damage per block broken.");
                                    return tool;
                                }))
                        )
                )
                .then(literal("-view")
                        .executes(this::handleView)
                )
                .then(literal("-clear")
                        .executes(context -> updateTool(context, tool -> {
                            Utils.tell(context.getSource(), "<primary>Your item is no longer a tool!");
                            return null;
                        }))
                );
    }

    private int handleView(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        final ToolComponent component = meta.getTool();

        final StringBuilder builder = new StringBuilder();

        if (component.getRules().isEmpty()) {
            builder.append("<dark_gray>Empty...</dark_gray>");
        } else {
            component.getRules().forEach(rule -> builder.append("<hover:show_text:'").append(convertToHover(rule))
                    .append("'><click:COPY_TO_CLIPBOARD:'").append(convertToClick(rule)).append("'><primary>Blocks: <secondary>").append(rule.getBlocks().size()).append("</secondary>, Speed: <secondary>")
                    .append(rule.getSpeed()).append("</secondary></click></hover><dark_gray>,</dark_gray> "));
        }

        Utils.tell(user, """
                <primary>Default Speed: <secondary>{speed}</secondary>
                <primary>Damage Per Block: <secondary>{dmg}</secondary>
                \s
                Rules: <secondary>{rules}</secondary>
                <info>Hover over the rule to see the full rule, click to copy the command to your clipboard."""
                .replace("{speed}", String.valueOf(component.getDefaultMiningSpeed()))
                .replace("{dmg}", String.valueOf(component.getDamagePerBlock()))
                .replace("{rules}", StringUtils.substringBeforeLast(builder.toString(), ","))
        );
        return 1;
    }

    private int updateTool(final @NotNull CommandContext<CommandSourceStack> context, UnaryOperator<ToolComponent> function) {
        return updateItemMeta(context, (meta) -> {
            meta.setTool(function.apply(meta.getTool()));
            return meta;
        });
    }

    private int updateItemMeta(final @NotNull CommandContext<CommandSourceStack> context, UnaryOperator<ItemMeta> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        stack.setItemMeta(function.apply(meta));
        return 1;
    }

    private int addRuleBlocks(final CommandContext<CommandSourceStack> context, float speed, boolean correct) {
        final List<Material> blocks = new ArrayList<>();
        final String blocksString = StringArgumentType.getString(context, "blocks").replace("\"", "");
        final String[] blocksArray = blocksString.split("[,\\s]");
        for (String block : blocksArray) {
            final Material material = Material.matchMaterial(block);
            if (material != null) {
                blocks.add(material);
            } else {
                Utils.log("Skipping unknown material: " + block);
            }
        }

        return updateTool(context, tool -> {
            if (speed == 0) {
                tool.addRule(blocks, tool.getDefaultMiningSpeed(), correct);
            } else {
                tool.addRule(blocks, speed, correct);
            }

            Utils.tell(context.getSource(), "<primary>Added the tool rule to your item. <info><hover:show_text:'" + convertToHover(tool.getRules().getLast()) + "'>Hover me for the full rule added!");
            return tool;
        });
    }

    private int addRuleResourceOrTag(final CommandContext<CommandSourceStack> context, float speed, boolean correct) throws CommandSyntaxException {
        final ResourceOrTagKeyArgument.Result<Block> result = getResourceOrTagKey(context, "block or tag", Registries.BLOCK, ERROR_INVALID_BLOCK);
        final Either<ResourceKey<Block>, TagKey<Block>> either = result.unwrap();

        // Actual block.
        if (either.left().isPresent()) {
            final String block = either.left().get().location().toString();
            final Material material = Material.matchMaterial(block);

            if (material == null) {
                throw ERROR_INVALID_BLOCK.create(block);
            }

            return updateTool(context, tool -> {
                if (speed == 0) {
                    tool.addRule(material, tool.getDefaultMiningSpeed(), correct);
                } else {
                    tool.addRule(material, speed, correct);
                }

                Utils.tell(context.getSource(), "<primary>Added the tool rule to your item. <info><hover:show_text:'" + convertToHover(tool.getRules().getLast()) + "'>Hover me for the full rule added!");
                return tool;
            });
        }
        // Block tag; uses the client may differ from server.
        else if (either.right().isPresent()) {
            // NMS Tag.
            final TagKey<Block> tag = either.right().get();
            // Bukkit tag.
            final Tag<Material> blockTag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, Objects.requireNonNull(NamespacedKey.fromString(tag.location().toString())), Material.class);
            if (blockTag == null) {
                throw ERROR_INVALID_BLOCK.create(tag.location().toString());
            }

            return updateTool(context, tool -> {
                if (speed == 0) {
                    tool.addRule(blockTag, tool.getDefaultMiningSpeed(), correct);
                } else {
                    tool.addRule(blockTag, speed, correct);
                }

                Utils.tell(context.getSource(), "<primary>Added the tool rule to your item. <info><hover:show_text:'" + convertToHover(tool.getRules().getLast()) + "'>Hover me for the full rule added!");
                return tool;
            });
        }

        return 0;
    }

    private @NotNull String convertToClick(final ToolComponent.@NotNull ToolRule rule) {
        return "/ie tool add " + String.join(",", rule.getBlocks().stream().map(Material::name).toList()) + " " + rule.getSpeed() + " " + (rule.isCorrectForDrops() == null ? "" : rule.isCorrectForDrops());
    }

    private @NotNull String convertToHover(final ToolComponent.@NotNull ToolRule rule) {
        return """
                <primary>Blocks <info>(tag or block ids)</info>: <secondary>{blocks}</secondary>
                Speed: <secondary>{speed}</secondary>
                Correct for drops: <secondary>{correct}</secondary>
                """
                .replace("{blocks}", rule.getBlocks().isEmpty() ? "<red>None?</red>" : String.join(",", rule.getBlocks().stream().map(Material::name).toList()))
                .replace("{speed}", String.valueOf(rule.getSpeed()))
                .replace("{correct}", rule.isCorrectForDrops() == null ? "<gray>null</gray>" : Boolean.TRUE.equals(rule.isCorrectForDrops()) ? "<green>yes</green>" : "<red>no</red>");
    }

    // Gets the lore lines off an item.
    private @NotNull List<String> getRuleAmount(ItemMeta meta) {
        return new ArrayList<>(meta != null && !meta.getTool().getRules().isEmpty() ? IntStream.range(1, meta.getTool().getRules().size() + 1).mapToObj(Integer::toString).toList() : List.of("0"));
    }

    private <T> ResourceOrTagKeyArgument.Result<T> getResourceOrTagKey(@NotNull CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryRef,
                                                                       DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        final ResourceOrTagKeyArgument.Result<?> result = context.getArgument(name, ResourceOrTagKeyArgument.Result.class);
        final Optional<ResourceOrTagKeyArgument.Result<T>> optional = result.cast(registryRef);
        return optional.orElseThrow(() -> invalidException.create(result));
    }

}
