package com.github.duync.jmeterviewer;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts.Label;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public final class JMeterFileType extends LanguageFileType {
    public static final JMeterFileType INSTANCE = new JMeterFileType();

    private JMeterFileType() {
        super(JMeterLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "JMeter Test Plan";
    }

    @Override
    public @Label @NotNull String getDescription() {
        return "Apache JMeter test plan";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "jmx";
    }

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public @Nullable String getCharset(@NotNull VirtualFile file, byte @NotNull [] content) {
        return "UTF-8";
    }
}
