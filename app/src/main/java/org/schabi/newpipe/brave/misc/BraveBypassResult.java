package org.schabi.newpipe.brave.misc;

public record BraveBypassResult(
        boolean success,
        String content,
        String cookies
) {
}
