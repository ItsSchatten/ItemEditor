package com.itsschatten.itemeditor.commands.subcommands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;

public final class SkinSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie skin <secondary><uuid|username|texture url|sound> [sound]</secondary>").hoverEvent(StringUtil.color("""
                        <primary>Sets a skull's texture to the one provided.
                        \s
                        ◼ <secondary><uuid|username|texture url><required></secondary> Either a UUID, a username, or a textures.minecraft.net url.
                        ◼ <secondary>sound <sound><required></secondary> Set the note block sound.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie skin "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("skin")
                .then(literal("sound")
                        .then(literal("-clear")
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    final ItemStack hand = user.getItemInHand();
                                    if (ItemValidator.isInvalid(hand)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    if (hand.hasData(DataComponentTypes.NOTE_BLOCK_SOUND)) {
                                        hand.resetData(DataComponentTypes.NOTE_BLOCK_SOUND);
                                        Utils.tell(user, "<primary>Removed your skull's note block sound!");
                                    } else {
                                        Utils.tell(user, "<primary>Your skull doesn't have a note block sound!");
                                    }

                                    return 1;
                                })
                        )
                        .then(literal("-view")
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    final ItemStack hand = user.getItemInHand();
                                    if (ItemValidator.isInvalid(hand)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    if (hand.hasData(DataComponentTypes.NOTE_BLOCK_SOUND)) {
                                        Utils.tell(user, "<primary>Your skull's current note block sound is <secondary>" + Objects.requireNonNull(hand.getData(DataComponentTypes.NOTE_BLOCK_SOUND)).asMinimalString() + "</secondary>!");
                                    } else {
                                        Utils.tell(user, "<primary>Your skull doesn't have a note block sound!");
                                    }

                                    return 1;
                                })
                        )
                        .then(literal("-play")
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    final ItemStack hand = user.getItemInHand();
                                    if (ItemValidator.isInvalid(hand)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    if (hand.hasData(DataComponentTypes.NOTE_BLOCK_SOUND)) {
                                        user.playSound(Sound.sound(Objects.requireNonNull(hand.getData(DataComponentTypes.NOTE_BLOCK_SOUND)), Sound.Source.RECORD, 1f, 1f));
                                        Utils.tell(user, "<primary>Played your skulls note block sound too you!");
                                    } else {
                                        Utils.tell(user, "<primary>Your item doesn't have a tooltip style!");
                                    }

                                    return 1;
                                })
                        )
                        .then(argument("sound", ArgumentTypes.resourceKey(RegistryKey.SOUND_EVENT))
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    // Get the item's meta and check if it's null, it really shouldn't be but safety.
                                    if (!(stack.getItemMeta() instanceof SkullMeta)) {
                                        Utils.tell(user, "<red>Your item is not a skull!");
                                        return 0;
                                    }

                                    final Key key = context.getArgument("sound", Key.class);
                                    stack.setData(DataComponentTypes.NOTE_BLOCK_SOUND, key);
                                    Utils.tell(user, "<primary>Set your skulls note block sound to <secondary>" + key.asMinimalString() + "</secondary>!");
                                    return 1;
                                })
                        )
                )
                .then(literal("-remove_name")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            if (!(stack.getItemMeta() instanceof final SkullMeta meta)) {
                                Utils.tell(user, "<red>Your item is not a skull!");
                                return 0;
                            }

                            final PlayerProfile textures = meta.getPlayerProfile();
                            assert textures != null;
                            stack.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile().name(null).addProperties(textures.getProperties()).build());
                            return 1;
                        })
                )
                .then(argument("uuid", ArgumentTypes.uuid())
                        .executes(context -> {
                            final UUID uuid = context.getArgument("uuid", UUID.class);
                            final PlayerProfile profile = Bukkit.createProfile(uuid);

                            Utils.tell(context.getSource(), "<primary>Updated your skull to use the skin of <secondary>" + uuid + "</secondary>!");
                            return updateTextures(context, profile);
                        })
                )
                .then(argument("username", StringArgumentType.word())
                        .executes(context -> {
                            final String username = context.getArgument("username", String.class);
                            final long start = System.currentTimeMillis();
                            Utils.tell(context.getSource(), "<gray><i>Looking up player skin by the name '" + username + "'...");

                            final Player user = (Player) context.getSource().getSender();

                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            if (!(stack.getItemMeta() instanceof final SkullMeta meta)) {
                                Utils.tell(user, "<red>Your item is not a skull!");
                                return 0;
                            }

                            // Async get the offline player by name.
                            Bukkit.getScheduler().runTaskAsynchronously(Utils.getInstance(), () -> {
                                meta.setOwningPlayer(Bukkit.getOfflinePlayer(username));
                                stack.setItemMeta(meta);
                                Utils.tell(user, "<primary>Updated your skull to use the skin of <secondary>" + username + "</secondary>!");
                                Utils.tell(user, "<gray><i>Took " + (System.currentTimeMillis() - start) + "ms!");
                            });
                            return 1;
                        })
                )
                .then(argument("url", StringArgumentType.string())
                        .executes(context -> {
                            final String url = context.getArgument("url", String.class);

                            if (url.startsWith("http") && url.contains("textures.minecraft.net")) {
                                // Create a new profile and get the textures.
                                final PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                                final PlayerTextures textures = profile.getTextures();

                                // Attempt skull url setting.
                                try {
                                    textures.setSkin(new URI(url).toURL());
                                } catch (URISyntaxException | MalformedURLException e) {
                                    Utils.logError(e);
                                    Utils.sendDeveloperErrorMessage((Player) context.getSource().getSender(), e);
                                    Utils.tell(context.getSource(), "<red>Failed to load skull texture: " + url);
                                    return 0;
                                }

                                // Set the textures.
                                profile.setTextures(textures);
                                Utils.tell(context.getSource(), "<primary>Updated your skull to use the skin <secondary>" + url + "</secondary>!");

                                return updateTextures(context, profile);
                            } else {
                                Utils.tell(context.getSource(), "<red>Failed to load skull texture: " + url);
                                return 0;
                            }
                        })
                );
    }

    private int updateTextures(final @NotNull CommandContext<CommandSourceStack> context, final PlayerProfile profile) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final SkullMeta meta)) {
            Utils.tell(user, "<red>Your item is not a skull!");
            return 0;
        }

        meta.setPlayerProfile(profile);
        stack.setItemMeta(meta);
        return 1;
    }
}
