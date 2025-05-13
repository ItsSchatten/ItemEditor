package com.itsschatten.itemeditor.menus;

import com.itsschatten.itemeditor.utils.StringHelper;
import com.itsschatten.yggdrasil.WrapUtils;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.SkinTexture;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.items.manipulators.ColorManipulator;
import com.itsschatten.yggdrasil.items.manipulators.SkullManipulator;
import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.MenuTriggerButton;
import com.itsschatten.yggdrasil.menus.types.PaginatedMenu;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.InventorySize;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.registry.set.RegistryKeySet;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ConsumableEffectMenu extends PaginatedMenu<MenuHolder, ConsumeEffect> {

    final List<ConsumeEffect> effects;
    final ItemStack stack;
    final Consumable.Builder consumable;

    public ConsumableEffectMenu(final ItemStack stack, final @NotNull Consumable.Builder consumable) {
        super(null, "Consumable effect Menu", InventorySize.FULL, new ArrayList<>(consumable.build().consumeEffects()));

        this.stack = stack;
        // This is here to make this list modifiable.
        // Otherwise, removing the effects using this menu would be impossible.
        this.effects = new ArrayList<>(consumable.build().consumeEffects());
        this.consumable = Consumable.consumable()
                .consumeSeconds(consumable.build().consumeSeconds())
                .hasConsumeParticles(consumable.build().hasConsumeParticles())
                .animation(consumable.build().animation())
                .sound(consumable.build().sound());

        setHideNav(true);
    }

    @Override
    public void drawExtra() {
        setRow(5, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    @Override
    public List<Button<MenuHolder>> makeButtons() {
        return List.of(new MenuTriggerButton<>() {
            @Override
            public Menu getMenu(MenuHolder user, ClickType click) {
                return new ConsumableEffectCreateMenu(ConsumableEffectMenu.this);
            }

            @Override
            public ItemCreator createItem() {
                return ItemCreator.of(Material.PLAYER_HEAD)
                        .name("Add New Effect")
                        .lore("Adds a new consume effect to your consumable.")
                        .manipulator(new SkullManipulator(new SkinTexture(UUID.fromString("934efd8c-1026-4ced-b56b-d9ba91221bbb"), "b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f")))
                        .build();
            }

            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(5, 1);
            }
        });
    }

    @Override
    public ItemCreator convertToStack(@NotNull ConsumeEffect consumeEffect) {
        return switch (consumeEffect) {
            case final ConsumeEffect.ApplyStatusEffects effect -> {
                final List<PotionEffect> effects = effect.effects();
                final List<String> lore = new ArrayList<>(List.of(
                        "<info>Info Legend: Has Particles, Has Icon, Is Ambient",
                        "<dark_aqua>Probability to apply <arrow> <secondary>" + (effect.probability() * 100.0F) + "%",
                        "<dark_aqua>Applies Effects <arrow>"
                ));
                lore.addAll(WrapUtils.convertStringToList("<secondary>" + effectsToString(effects)));
                lore.addAll(List.of(
                        "",
                        "<yellow>Left-Click<gray> to remove this effect."
                ));

                final List<Color> colors = new ArrayList<>(effect.effects().stream().map(eff -> eff.getType().getColor()).toList());
                final Color color = colors.size() <= 1 ? colors.getFirst() : colors.removeFirst().mixColors(colors.toArray(new Color[0]));

                yield ItemCreator.of(Material.POTION).display("<light_purple>Apply Status Effects").lore(lore)
                        .options(ItemOptions.builder().hiddenComponent(DataComponentTypes.POTION_CONTENTS))
                        .manipulator(new ColorManipulator(color)).build();
            }
            case final ConsumeEffect.ClearAllStatusEffects ignored ->
                    ItemCreator.of(Material.MILK_BUCKET).name("Clear All Effects").lore(
                            List.of(
                                    "<yellow>Left-Click<gray> to remove this effect."
                            )
                    ).build();
            case final ConsumeEffect.PlaySound effect ->
                    ItemCreator.of(Material.JUKEBOX).name("<green>Play Sound").lore(
                            List.of(
                                    "<dark_aqua>Sound <arrow> <secondary>" + effect.sound().asMinimalString(),
                                    "",
                                    "<yellow>Left-Click<gray> to remove this effect."
                            )
                    ).build();
            case final ConsumeEffect.RemoveStatusEffects effect -> {
                final List<String> lore;
                if (effect.removeEffects().size() > 2) {
                    lore = new ArrayList<>();
                    lore.add("<dark_aqua>Clears Effects <arrow>");
                    lore.addAll(WrapUtils.convertStringToList("<secondary>" + registrySetToString(effect.removeEffects())));
                    lore.addAll(List.of(
                            "",
                            "<yellow>Left-Click<gray> to remove this effect."
                    ));
                } else lore = List.of(
                        "<dark_aqua>Clears Effects <arrow> <secondary>" + registrySetToString(effect.removeEffects()),
                        "",
                        "<yellow>Left-Click<gray> to remove this effect."
                );


                yield ItemCreator.of(Material.BOWL).name("<red>Remove Status Effects").lore(lore).build();
            }
            case final ConsumeEffect.TeleportRandomly effect ->
                    ItemCreator.of(Material.CHORUS_FRUIT).name("<dark_purple>Random Teleport").lore(
                            List.of(
                                    "<dark_aqua>Diameter <arrow> <secondary>" + effect.diameter(),
                                    "",
                                    "<yellow>Left-Click<gray> to remove this effect."
                            )
                    ).build();
            default -> throw new IllegalArgumentException("Unknown effect: " + consumeEffect);
        };
    }

    @Override
    public void postDisplay(MenuHolder holder) {
        updatePages(effects);
    }

    @Override
    public void onClose(MenuHolder holder) {
        // We must check if we aren't opening new, if we didn't, the data would be updated everytime we switch from this menu.
        // Which isn't a very good experience.
        // We also check if the item is consumable because then we can update it. If it doesn't, we use the default consumable.
        if (this.stack.hasData(DataComponentTypes.CONSUMABLE))
            this.stack.setData(DataComponentTypes.CONSUMABLE, this.consumable.addEffects(effects).build());
        else if (!this.effects.isEmpty()) {
            this.stack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().addEffects(effects).build());
        }
    }

    @Override
    public void onClickPageItem(MenuHolder holder, ConsumeEffect consumeEffect, @NotNull ClickType clickType) {
        if (clickType.isLeftClick()) {
            effects.remove(consumeEffect);
            cleanUpdatePages(effects);
        }
    }

    private @NotNull String effectsToString(final @NotNull List<PotionEffect> effects) {
        final StringBuilder builder = new StringBuilder();
        for (final PotionEffect effect : effects) {
            builder.append(StringHelper.potionEffectToString(effect)).append("<gray>,</gray> <br>");
        }

        return builder.substring(0, builder.toString().lastIndexOf(","));
    }

    private @NotNull String registrySetToString(@NotNull RegistryKeySet<@NotNull PotionEffectType> set) {
        final StringBuilder builder = new StringBuilder();
        set.values().forEach(type -> builder.append(type.key().asMinimalString()).append("<gray>,</gray> "));
        return builder.toString().lastIndexOf(",") != -1 ? builder.substring(0, builder.toString().lastIndexOf(",")) : builder.toString();
    }

}
