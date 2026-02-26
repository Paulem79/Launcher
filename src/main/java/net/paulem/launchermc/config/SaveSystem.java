package net.paulem.launchermc.config;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

public final class SaveSystem {
    @Getter
    @Setter
    private Runnable saveUi;
    @Getter
    private final Collection<Runnable> runs;

    public SaveSystem() {
        this.runs = new ArrayList<>();
    }
    
    public void add(Runnable run) {
        runs.add(run);
    }
}
