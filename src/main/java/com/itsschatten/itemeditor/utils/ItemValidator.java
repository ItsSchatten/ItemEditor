package com.itsschatten.itemeditor.utils;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public final class ItemValidator {

    /**
     * Check if an {@link ItemStack} matches the provided predicate.
     *
     * @param stack    The {@link ItemStack} to check validity for.
     * @param validity The {@link Predicate} to test for the validity check.
     * @return Returns whatever {@link Predicate#test(Object)}, where the object is the ItemStack.
     */
    public static boolean isValid(final ItemStack stack, final @NotNull Predicate<ItemStack> validity) {
        return validity.test(stack);
    }

    /**
     * Check if an {@link ItemStack} is not null and is not empty.
     *
     * @param stack The {@link ItemStack} to check.
     * @return Returns if this {@link ItemStack} is not null and isn't empty.
     */
    public static boolean isInvalid(final ItemStack stack) {
        return isInvalid(stack, (item) -> item != null && !item.isEmpty());
    }

    /**
     * Check if an {@link ItemStack} doesn't match the provided predicate.
     *
     * @param stack    The {@link ItemStack} to check validity for.
     * @param validity The {@link Predicate} to test for the validity check.
     * @return Returns the negated version of {@link Predicate#test(Object)}, where the object is the ItemStack.
     */
    public static boolean isInvalid(final ItemStack stack, final @NotNull Predicate<ItemStack> validity) {
        return validity.negate().test(stack);
    }

}
