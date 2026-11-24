import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.function.Function;

final class MarketplaceStore {
    private final Path dataFile;
    private final Path lockFile;

    MarketplaceStore(Path path) {
        dataFile = path.toAbsolutePath().normalize();
        if (dataFile.getFileName() == null) {
            throw new IllegalArgumentException("Choose a file for marketplace data.");
        }
        lockFile = dataFile.resolveSibling(dataFile.getFileName() + ".lock");
    }

    Marketplace load() throws IOException {
        Properties data = new Properties();
        try (var reader = Files.newBufferedReader(dataFile)) {
            data.load(reader);
            return MarketplacePropertiesCodec.decode(data);
        } catch (NoSuchFileException exception) {
            return new Marketplace();
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid marketplace data in " + dataFile + ". " + exception.getMessage(),
                    exception);
        }
    }

    <T> T update(Function<Marketplace, T> change) throws IOException {
        Files.createDirectories(dataFile.getParent());
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            // Keep the read, change, and save together when multiple CLI processes use the same file.
            var lock = channel.lock();
            try {
                Marketplace marketplace = load();
                T result = change.apply(marketplace);
                save(marketplace);
                return result;
            } finally {
                lock.release();
            }
        }
    }

    private void save(Marketplace marketplace) throws IOException {
        Properties data = MarketplacePropertiesCodec.encode(marketplace);
        Path temporary = Files.createTempFile(dataFile.getParent(), "marketplace-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary)) {
                data.store(writer, "UCampus Mart");
            }
            Files.move(temporary, dataFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
