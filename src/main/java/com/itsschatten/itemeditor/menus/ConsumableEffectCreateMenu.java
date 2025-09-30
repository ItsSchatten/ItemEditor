package com.itsschatten.itemeditor.menus;

import com.itsschatten.itemeditor.utils.ConsumeEffectOptions;
import com.itsschatten.itemeditor.utils.StringHelper;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.anvilgui.AnvilGUI;
import com.itsschatten.yggdrasil.anvilgui.interfaces.Response;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.items.manipulators.ColorManipulator;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.Buttons;
import com.itsschatten.yggdrasil.menus.buttons.premade.PageNavigationButton;
import com.itsschatten.yggdrasil.menus.buttons.premade.ReturnButton;
import com.itsschatten.yggdrasil.menus.types.PageMenu;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.InventorySize;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import com.itsschatten.yggdrasil.menus.utils.MenuPage;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistrySet;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class ConsumableEffectCreateMenu extends PageMenu<MenuHolder> {

    private final ConsumableEffectMenu effectsMenu;

    ConsumeEffectOptions options;

    private boolean configuring;

    public ConsumableEffectCreateMenu(@NotNull ConsumableEffectMenu parent) {
        super(parent, "Effect Creation Menu", InventorySize.FULL, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));

        this.effectsMenu = parent;
    }

    @Override
    public void postDisplay(MenuHolder holder) {
        // Called to update the page's view to ensure states are properly shown.
        refreshPage();
    }

    @Contract(pure = true)
    @Override
    public @NotNull @Unmodifiable List<Button<MenuHolder>> makeButtons() {
        return List.of();
    }

    @Override
    public void drawExtra() {
        fill(UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
        setRow(1, UtilityItems.makeFiller(Material.YELLOW_STAINED_GLASS_PANE));
    }

    @Override
    public @NotNull @Unmodifiable List<MenuPage<MenuHolder>> makePages() {
        final List<String> lore = new ArrayList<>();
        if (options instanceof ConsumeEffectOptions.ApplyStatusEffectsOptions(
                final List<PotionEffect> effects, final float probability
        )) {
            lore.add("<value>Effects <arrow> <secondary>" + StringHelper.firstEffect(effects));
            if (!effects.isEmpty()) {
                effects.forEach(effect -> lore.add("<secondary>" + StringHelper.potionEffectToString(effect)));
            }
            lore.add("");
            lore.add("<value>Probability <arrow> <secondary>" + (probability * 100F) + "%");
            lore.add("<yellow>Click<secondary> to change this list.");
        } else if (options instanceof ConsumeEffectOptions.RemoveStatusEffectsOptions(
                final List<PotionEffectType> effects
        )) {
            final List<PotionEffectType> fixed = new ArrayList<>(effects);
            lore.add("<value>Effects <arrow> <secondary>" + StringHelper.firstTwoEffectTypes(fixed));
            if (!effects.isEmpty()) {
                fixed.forEach(effect -> lore.add("<secondary>" + effect.key().asMinimalString()));
            }
            lore.add("");
            lore.add("<yellow>Click<secondary> to change this list.");
        } else {
            lore.add("You should add some effects so they can show up here!");
        }

        return List.of(
                MenuPage.builder()
                        .navButton(PageNavigationButton.builder()
                                .runnable((holder, menu, click) -> options = null)
                                .pageNumber(1)
                                .item((page) -> ItemCreator.of(Material.POTION).display("<light_purple>Apply Status Effects").lore("<secondary>When eaten your consumable can apply", "<secondary>a list of potion effects to the consumer.").options(ItemOptions.builder().hiddenComponent(DataComponentTypes.POTION_CONTENTS).glow(page == 1).build()).manipulator(new ColorManipulator("#ff55ff")).supplier())
                                .position(0, 0).build())
                        .button(Buttons.menuTrigger()
                                .menu((holder, clickType) -> new EffectListMenu(this, options instanceof final ConsumeEffectOptions.ApplyStatusEffectsOptions apply ? apply.effects() : List.of()))
                                .item(() -> ItemCreator.of(Material.CHEST).name("<primary>Effects").lore(lore))
                                .position(3, 2)
                                .build())
                        .build(),
                MenuPage.builder()
                        .navButton(PageNavigationButton.builder()
                                .runnable((holder, menu, click) -> options = null)
                                .item((page) -> ItemCreator.of(Material.MILK_BUCKET).display("Clear All Effects").lore("<secondary>When eaten your consumable will remove", "<secondary>all status effects from the consumer.").options(ItemOptions.builder().glow(page == 2).build()).supplier())
                                .pageNumber(2)
                                .position(0, 2).build())
                        .button(Buttons.dynamic()
                                .item(() -> ItemCreator.of(options instanceof ConsumeEffectOptions.ClearAllStatusEffectsOptions ? Material.BUCKET : Material.MILK_BUCKET)
                                        .name("<green>Click to Apply").lore("<secondary>Click to apply the clear all effects", "<secondary>consume effect."))
                                .onClick((holder, menu, click) -> {
                                    if (options instanceof ConsumeEffectOptions.ClearAllStatusEffectsOptions) {
                                        holder.tell("<green>This effect is already selected! <gold>Switch pages if you wish to remove it or close/click the return button to add the effect.");
                                        return;
                                    }

                                    options = new ConsumeEffectOptions.ClearAllStatusEffectsOptions();
                                    holder.tell("<green>Your consumable will now remove all effects from the consumer.");
                                })
                                .position(3, 4)
                                .build()
                        )
                        .build(),
                MenuPage.builder()
                        .navButton(PageNavigationButton.builder()
                                .runnable((holder, menu, click) -> options = null)
                                .item((page) -> ItemCreator.of(Material.JUKEBOX).display("<green>Play Sound").lore("<secondary>When eaten your consumable will play", "<secondary>a sound to the consumer.").options(ItemOptions.builder().glow(page == 3).build()).supplier())
                                .pageNumber(3)
                                .position(0, 4).build())
                        .button(Buttons.menuTrigger()
                                .menu((holder, clickType) -> new SoundListMenu(this))
                                .item(() -> ItemCreator.of(Material.NOTE_BLOCK).display("<primary>Sound").lore("<secondary>The sound to play.", "", "<value>Sound <arrow> <secondary>" + (options != null && options instanceof ConsumeEffectOptions.PlaySoundOptions(
                                        net.kyori.adventure.key.Key sound
                                ) ? sound.asMinimalString() : "entity.generic.eat"), "", "<yellow>Click <primary>to change the sound!"))
                                .position(3, 4)
                                .build())
                        .build(),
                MenuPage.builder()
                        .navButton(PageNavigationButton.builder()
                                .runnable((holder, menu, click) -> options = null)
                                .item((page) -> ItemCreator.of(Material.BOWL).display("<red>Remove Status Effects").lore("<secondary>When eaten your consumable will remove", "<secondary>a list of potion effects from the consumer.").options(ItemOptions.builder().glow(page == 4).build()).supplier())
                                .pageNumber(4)
                                .position(0, 6).build())
                        .button(Buttons.menuTrigger()
                                .menu((holder, clickType) -> new EffectTypeListMenu(this, options instanceof ConsumeEffectOptions.RemoveStatusEffectsOptions(
                                        List<PotionEffectType> effects
                                ) ? effects : List.of()))
                                .item(() -> ItemCreator.of(Material.CHEST).name("<primary>Effects").lore(lore))
                                .position(3, 4)
                                .build()
                        )
                        .build(),
                MenuPage.builder()
                        .navButton(PageNavigationButton.builder()
                                .runnable((holder, menu, click) -> options = null)
                                .item((page) -> ItemCreator.of(Material.CHORUS_FRUIT).display("<dark_purple>Random Teleport").lore("<secondary>When eaten your consumable will randomly", "<secondary>teleport the consumer within a set diameter.").options(ItemOptions.builder().glow(page == 5).build()).supplier())
                                .pageNumber(5)
                                .position(0, 8).build())
                        .button(Buttons.button()
                                .item(() -> ItemCreator.of(Material.OAK_SIGN).display("<primary>Diameter").lore("<secondary>The diameter of of the random teleport.", "", "<value>Diameter <arrow> <secondary>" + (options != null && options instanceof ConsumeEffectOptions.RandomTeleportOptions(
                                        float diameter
                                ) ? String.valueOf(diameter).replace(".0", "") : "16"), "", "<yellow>Click <primary>to change the diameter!"))
                                .onClick((holder, menu, click) -> {
                                    configuring = true;
                                    AnvilGUI.builder().holder(holder).plugin(Utils.getInstance())
                                            .title("Enter Diameter")
                                            .clickHandler((integer, snapshot) -> CompletableFuture.completedFuture(Collections.singletonList(Response.openMenu(menu, holder))))
                                            .onClose(snapshot -> {
                                                configuring = false;
                                                if (!snapshot.text().isBlank() && NumberUtils.isCreatable(snapshot.text())) {
                                                    options = new ConsumeEffectOptions.RandomTeleportOptions(NumberUtils.toFloat(snapshot.text()));
                                                }
                                            })
                                            .response(Response.openMenu(menu, holder))
                                            .open(holder);
                                })
                                .position(3, 4)
                                .build())
                        .build()
        );
    }

    @Override
    public @Nullable ReturnButton.ReturnButtonBuilder<MenuHolder> getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).position(InventoryPosition.of(rows() - 1, 0));
    }

    @Override
    public void beforeDispose(MenuHolder holder) {
        passOptions();
    }

    @Override
    public void onClose(MenuHolder user) {
        if (!configuring) {
            passOptions();

            // A tick must delay this. Otherwise, the close call will be called continuously.
            Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> effectsMenu.switchMenu(user, this), 1L);
        }
    }

    private void passOptions() {
        if (options != null) {
            switch (options) {
                case final ConsumeEffectOptions.ApplyStatusEffectsOptions apply ->
                        effectsMenu.effects.add(ConsumeEffect.applyStatusEffects(apply.effects(), apply.probability()));

                case final ConsumeEffectOptions.ClearAllStatusEffectsOptions ignored ->
                        effectsMenu.effects.add(ConsumeEffect.clearAllStatusEffects());

                case final ConsumeEffectOptions.PlaySoundOptions sounds ->
                        effectsMenu.effects.add(ConsumeEffect.playSoundConsumeEffect(sounds.sound()));

                case final ConsumeEffectOptions.RemoveStatusEffectsOptions remove ->
                        effectsMenu.effects.add(ConsumeEffect.removeEffects(RegistrySet.keySetFromValues(RegistryKey.MOB_EFFECT, remove.effects())));

                case final ConsumeEffectOptions.RandomTeleportOptions random ->
                        effectsMenu.effects.add(ConsumeEffect.teleportRandomlyEffect(random.diameter()));

                default -> {
                }
            }
        }
    }

}
