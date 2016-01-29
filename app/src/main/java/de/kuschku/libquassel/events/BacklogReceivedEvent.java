package de.kuschku.libquassel.events;

import android.support.annotation.IntRange;

@SuppressWarnings("WeakerAccess")
public class BacklogReceivedEvent {
    @IntRange(from = -1)
    public final int bufferId;

    public BacklogReceivedEvent(int bufferId) {
        this.bufferId = bufferId;
    }
}