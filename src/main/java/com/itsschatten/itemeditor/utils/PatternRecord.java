package com.itsschatten.itemeditor.utils;

import org.bukkit.block.banner.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Record to hold a {@link Pattern banner pattern} and it's location in a {@link List}.
 *
 * @param location    The location of this pattern in a list.
 * @param fullPattern The {@link Pattern} for the banner, this should also support customizable banner patterns.
 */
public record PatternRecord(int location, Pattern fullPattern) {

    /**
     * Utility method to convert a list of {@link Pattern}s to a list of {@link PatternRecord}s.
     *
     * @param patterns A list of {@link Pattern}s to convert.
     * @return Returns a new {@link ArrayList} of {@link PatternRecord}s, this list may be empty if the passed list is also empty.
     */
    @Contract(pure = true)
    public static @NotNull List<PatternRecord> convertToList(@NotNull List<Pattern> patterns) {
        final List<PatternRecord> records = new ArrayList<>();

        // Add all patterns as a new record with the id.
        for (int i = 0; i < patterns.size(); i++) {
            records.add(new PatternRecord(i, patterns.get(i)));
        }

        return records;
    }
}
