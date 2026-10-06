package duplicatefinder.media;

import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/** Locates FFmpeg or installs a verified portable copy in the user's cache on first use. */
public final class FfmpegTools {

    private static final String VERSION = "9.0.2";
    private static final String ARCHIVE_URL =
            "https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-" + VERSION + "-essentials_build.7z";
    private static final String SHA256 =
            "4705843ccaaf54257c16ad90f3e952ece33c17df964ecf7bfdbb0f49c7171077";
    private static final long MAX_ARCHIVE_BYTES = 200L * 1024 * 1024;
    private static final long MAX_EXECUTABLE_BYTES = 150L * 1024 * 1024;
    private static final Object INSTALL_LOCK = new Object();

    private FfmpegTools() {}

    public static Path ffmpeg() throws IOException {
        return toolsDirectory().resolve(executableName("ffmpeg"));
    }

    public static Path ffprobe() throws IOException {
        return toolsDirectory().resolve(executableName("ffprobe"));
    }

    private static Path toolsDirectory() throws IOException {
        Path fromPath = findOnPath();
        if (fromPath != null) return fromPath;
        if (!isWindows64Bit()) {
            throw new IOException("FFmpeg fehlt. Die automatische Einrichtung wird derzeit "
                    + "nur unter Windows 64-Bit unterstützt; bitte ffmpeg und ffprobe installieren.");
        }

        synchronized (INSTALL_LOCK) {
            Path cache = cacheDirectory();
            Files.createDirectories(cache);
            Path ffmpeg = cache.resolve(executableName("ffmpeg"));
            Path ffprobe = cache.resolve(executableName("ffprobe"));
            Path ready = cache.resolve(".ready");
            if (Files.isRegularFile(ready) && Files.isRegularFile(ffmpeg) && Files.isRegularFile(ffprobe)) {
                return cache;
            }

            Path lockPath = cache.resolve(".install.lock");
            try (FileChannel channel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                if (Files.isRegularFile(ready) && Files.isRegularFile(ffmpeg) && Files.isRegularFile(ffprobe)) {
                    return cache;
                }
                install(cache, ffmpeg, ffprobe, ready);
            }
            return cache;
        }
    }

    private static Path findOnPath() {
        String path = System.getenv("PATH");
        if (path == null) return null;
        String ffmpegName = executableName("ffmpeg");
        String ffprobeName = executableName("ffprobe");
        for (String directory : path.split(java.util.regex.Pattern.quote(System.getProperty("path.separator")))) {
            if (directory.isBlank()) continue;
            Path candidate = Path.of(directory);
            if (Files.isRegularFile(candidate.resolve(ffmpegName))
                    && Files.isRegularFile(candidate.resolve(ffprobeName))) {
                return candidate;
            }
        }
        return null;
    }

    private static void install(Path cache, Path ffmpeg, Path ffprobe, Path ready) throws IOException {
        Path archive = Files.createTempFile(cache, "ffmpeg-", ".7z.part");
        Path ffmpegPart = cache.resolve("ffmpeg.exe.part");
        Path ffprobePart = cache.resolve("ffprobe.exe.part");
        try {
            downloadAndVerify(archive);
            extractExecutables(archive, ffmpegPart, ffprobePart);
            Files.move(ffmpegPart, ffmpeg, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Files.move(ffprobePart, ffprobe, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Files.writeString(ready, VERSION, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IOException("FFmpeg konnte nicht automatisch eingerichtet werden. "
                    + "Prüfe die Internetverbindung oder installiere ffmpeg und ffprobe manuell.", e);
        } finally {
            Files.deleteIfExists(archive);
            Files.deleteIfExists(ffmpegPart);
            Files.deleteIfExists(ffprobePart);
        }
    }

    private static void downloadAndVerify(Path archive) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(ARCHIVE_URL))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "DuplicateFinder")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new IOException("Download-Server antwortete mit HTTP " + response.statusCode());
            }
            MessageDigest digest = sha256();
            long total = 0;
            try (InputStream input = response.body();
                 OutputStream output = Files.newOutputStream(archive, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_ARCHIVE_BYTES) throw new IOException("FFmpeg-Archiv ist unerwartet groß");
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
            }
            String actualHash = HexFormat.of().formatHex(digest.digest());
            if (!MessageDigest.isEqual(actualHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    SHA256.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                throw new IOException("FFmpeg-Archiv hat eine ungültige SHA-256-Prüfsumme");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download von FFmpeg wurde unterbrochen", e);
        }
    }

    private static void extractExecutables(Path archive, Path ffmpegPart, Path ffprobePart) throws IOException {
        boolean foundFfmpeg = false;
        boolean foundFfprobe = false;
        try (SevenZFile sevenZ = SevenZFile.builder().setFile(archive.toFile()).get()) {
            byte[] buffer = new byte[8192];
            org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry;
            while ((entry = sevenZ.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
                Path destination = name.endsWith("/bin/ffmpeg.exe") ? ffmpegPart
                        : name.endsWith("/bin/ffprobe.exe") ? ffprobePart : null;
                if (destination == null) continue;
                if (entry.getSize() <= 0 || entry.getSize() > MAX_EXECUTABLE_BYTES) {
                    throw new IOException("Ungültige Größe der FFmpeg-Programmdatei");
                }
                try (OutputStream output = Files.newOutputStream(destination,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    int read;
                    while ((read = sevenZ.read(buffer)) != -1) output.write(buffer, 0, read);
                }
                if (destination.equals(ffmpegPart)) foundFfmpeg = true;
                else foundFfprobe = true;
            }
        }
        if (!foundFfmpeg || !foundFfprobe) {
            throw new IOException("FFmpeg-Archiv enthält nicht beide benötigten Programme");
        }
    }

    private static MessageDigest sha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 wird von dieser Java-Installation nicht unterstützt", e);
        }
    }

    private static Path cacheDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".duplicate-finder")
                : Path.of(localAppData, "DuplicateFinder");
        return base.resolve("ffmpeg-" + VERSION);
    }

    private static boolean isWindows64Bit() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return os.startsWith("windows") && (arch.equals("amd64") || arch.equals("x86_64"));
    }

    private static String executableName(String baseName) {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")
                ? baseName + ".exe" : baseName;
    }
}
