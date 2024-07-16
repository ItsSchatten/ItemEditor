package com.itsschatten.itemeditor.commands.subcommands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
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
import java.util.UUID;

public class SkinSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie skin <secondary><uuid|username|texture url></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Sets a skull's texture to the one provided.
                        \s
                        ◼ <secondary><uuid|username|texture url><required></secondary> Either a UUID, a username, or a textures.minecraft.net url.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie skin "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("skin")
                .then(Commands.argument("uuid", ArgumentTypes.uuid())
                        .executes(context -> {
                            final UUID uuid = context.getArgument("uuid", UUID.class);
                            final PlayerProfile profile = Bukkit.createProfile(uuid);

                            Utils.tell(context.getSource(), "<primary>Updated your skull to use the skin of <secondary>" + uuid + "</secondary>!");
                            return updateTextures(context, profile);
                        })
                )
                .then(Commands.argument("username", StringArgumentType.word())
                        .executes(context -> {
                            final String username = context.getArgument("username", String.class);

                            Utils.tell(context.getSource(), "<gray><i>Looking up player skin by the name '" + username + "'...");

                            final Player user = (Player) context.getSource().getSender();

                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (stack.isEmpty()) {
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
                            });
                            return 1;
                        })
                )
                .then(Commands.argument("url", StringArgumentType.string())
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
        if (stack.isEmpty()) {
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
