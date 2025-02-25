package com.itsschatten.itemeditor;

import lombok.Getter;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An event that is called when ItemEditor configures the custom tags, this event does not contain the built-in tags of ItemEditor.
 */
@Getter
public class UpdateResolversEvent extends Event {

    // The list of handlers for this event.
    private static final HandlerList handlers = new HandlerList();

    /**
     * The tag resolver for this event.
     * --- GETTER ---
     * Get the {@link TagResolver} associated with this event.
     *
     * @return The {@link #resolver}.
     */
    private final TagResolver resolver;

    /**
     * Constructs the event.
     *
     * @param resolvers The {@link TagResolver}.
     */
    public UpdateResolversEvent(final TagResolver resolvers) {
        this.resolver = resolvers;
    }

    /**
     * Return the list of handlers for this event.
     *
     * @return Returns this event's {@link HandlerList}.
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * @return Returns this event's {@link HandlerList}.
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
