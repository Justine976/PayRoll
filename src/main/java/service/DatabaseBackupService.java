package service;

import config.DatabaseConfig;
import database.SQLiteConnectionManager;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class DatabaseBackupService {
    public void backupTo(Path destination) {
        if (destination == null) throw new IllegalArgumentException("Backup destination is required.");
        Path source = DatabaseConfig.databasePath();
        if (!Files.exists(source)) throw new IllegalStateException("Database file does not exist.");
        copyAtomic(source, destination);
    }

    public void restoreFrom(Path sourceBackup) {
        if (sourceBackup == null || !Files.exists(sourceBackup)) {
            throw new IllegalArgumentException("Valid backup file is required.");
        }
        Path target = DatabaseConfig.databasePath();
        Path temp = target.resolveSibling(target.getFileName() + ".restore.tmp");

        SQLiteConnectionManager.getInstance().close();
        copyAtomic(sourceBackup, temp);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw new IllegalStateException("Restore failed during atomic replace.", ex);
        }
    }

    private void copyAtomic(Path source, Path destination) {
        try {
            if (destination.getParent() != null) Files.createDirectories(destination.getParent());
            try (FileChannel in = FileChannel.open(source, StandardOpenOption.READ);
                    FileChannel out = FileChannel.open(destination,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE)) {
                long pos = 0;
                long size = in.size();
                while (pos < size) {
                    pos += in.transferTo(pos, size - pos, out);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("File copy failed.", ex);
        }
    }
}
