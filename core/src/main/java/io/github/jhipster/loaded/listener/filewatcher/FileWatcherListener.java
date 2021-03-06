package io.github.jhipster.loaded.listener.filewatcher;

import io.github.jhipster.loaded.FileSystemWatcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 *  All classes that implement this class will be called when a file changes (create or new).
 */
public interface FileWatcherListener {

    void setFileSystemWatcher(FileSystemWatcher fileSystemWatcher);

    boolean support(Path file, WatchEvent.Kind kind);

    void onChange(String parentFolder, Path file, WatchEvent.Kind kind);
}
