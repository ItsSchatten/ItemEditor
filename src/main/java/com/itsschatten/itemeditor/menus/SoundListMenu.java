package com.itsschatten.itemeditor.menus;

import com.google.common.collect.Lists;
import com.itsschatten.itemeditor.utils.ConsumeEffectOptions;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.menus.buttons.Buttons;
import com.itsschatten.yggdrasil.menus.buttons.premade.ReturnButton;
import com.itsschatten.yggdrasil.menus.types.PaginatedMenu;
import com.itsschatten.yggdrasil.menus.utils.IMenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;

public final class SoundListMenu extends PaginatedMenu<Sound> {

    final ConsumableEffectCreateMenu parent;

    public SoundListMenu(ConsumableEffectCreateMenu parent) {
        super(parent, Lists.newArrayList(Registry.SOUNDS).stream().filter(sound -> Registry.SOUNDS.getKey(sound) != null)
                .sorted(Comparator.comparing(o -> Objects.requireNonNull(Registry.SOUNDS.getKey(o)).key())).toList());
        this.parent = parent;

        setSize(54);
        setTitle("Sounds");
    }

    @Override
    public void drawExtra() {
        setRow(getInventory().getRows() - 1, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    @Override
    public void makeButtons() {
        registerButtons(Buttons.button()
                .item(ItemCreator.of(Material.BARRIER).name("<red>Stop Playing Sound").lore("<yellow>Click</yellow> to stop playing the sound.").supplier())
                .onClick((holder, menu, click) -> holder.getBase().stopAllSounds())
                .position(5, 7)
                .build());
    }

    @Override
    public @Nullable ReturnButton getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).toBuilder().position(getInventory().getRows() - 1, 0).build();
    }

    @Override
    public ItemCreator convertToStack(Sound sound) {
        return ItemCreator.of(Material.NOTE_BLOCK).name("<primary>" + Objects.requireNonNull(Registry.SOUNDS.getKey(sound)).key().asMinimalString() + "</primary>")
                .lore("<yellow>Left-Click</yellow> to select this sound.", "<yellow>Right-Click</yellow> to play this sound.")
                .options(ItemOptions.builder().glow(parent.options instanceof ConsumeEffectOptions.PlaySoundOptions(
                        net.kyori.adventure.key.Key sound1
                ) && Objects.requireNonNull(Registry.SOUNDS.getKey(sound)).key().equals(sound1)).build()).build();
    }

    @Override
    public void onClose(@NotNull IMenuHolder user) {
        user.getBase().stopAllSounds();
        if (!isOpeningNew()) {
            Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> Objects.requireNonNull(getParent()).displayTo(user), 1L);
        }
    }

    @Override
    public void onClickPageItem(IMenuHolder user, Sound sound, @NotNull ClickType click) {
        if (click.isRightClick()) {
            user.getBase().playSound(net.kyori.adventure.sound.Sound.sound().type(Objects.requireNonNull(Registry.SOUNDS.getKey(sound)).key()).build());
        } else {
            parent.options = new ConsumeEffectOptions.PlaySoundOptions(Objects.requireNonNull(Registry.SOUNDS.getKey(sound)).key());
            parent.switchMenu(user, this);
        }
    }
}
