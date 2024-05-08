package com.itsschatten.itemeditor.commands.subcommands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class SkinSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public SkinSubCommand(@NotNull CommandBase owningCommand) {
        super("skin", List.of("skull", "owner"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><uuid|username|texture url></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets a skull's texture to the one provided.
                \s
                ◼ <secondary><uuid|username|texture url><required></secondary> Either a UUID, a username, or a textures.minecraft.net url.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        if (!(stack.getItemMeta() instanceof final SkullMeta meta)) {
            returnTell("<red>Your item is not a skull!");
            return;
        }

        // Make sure we have args.
        if (args.length == 0) {
            returnTell("<red>You need to specify a player name, uuid, or Minecraft texture URL.");
            return;
        }

        // The value.
        final String value = args[0];

        if (value.startsWith("http") && value.contains("textures.minecraft.net")) {
            // Create a new profile and get the textures.
            final PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            final PlayerTextures textures = profile.getTextures();

            // Attempt skull url setting.
            try {
                textures.setSkin(new URL(value));
            } catch (MalformedURLException e) {
                Utils.logError(e);
                Utils.sendDeveloperErrorMessage(user, e);
                tell("<red>Failed to load skull texture: " + value);
                return;
            }

            // Set the textures.
            profile.setTextures(textures);
            meta.setPlayerProfile(profile);
            tell("<primary>Updated your skull to use the skin <secondary>" + value + "</secondary>!");
        } else {
            try {
                // Get the UUID, if it not a valid UUID, it will default to player name.
                final UUID uuid = UUID.fromString(value);
                final PlayerProfile profile = Bukkit.createProfile(uuid);
                // Set the owning player.
                meta.setPlayerProfile(profile);
                tell("<primary>Updated your skull to use the skin of <secondary>" + value + "</secondary>!");
            } catch (IllegalArgumentException ignored) {
                tell("<gray><i>Looking up player skin by the name " + value + "...");
                // Async get the offline player by name.
                Bukkit.getScheduler().runTaskAsynchronously(Utils.getInstance(), () -> {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(value));
                    stack.setItemMeta(meta);
                    tell("<primary>Updated your skull to use the skin of <secondary>" + value + "</secondary>!");
                });
                return;
            }
        }

        // Update the item meta.
        stack.setItemMeta(meta);
    }
}
