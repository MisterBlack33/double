package duplicatefinder.gui;

import duplicatefinder.media.VideoThumbnailExtractor;

import javax.swing.SwingWorker;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Lädt ein Video-Vorschaubild im Hintergrund, damit die EDT nicht blockiert. */
final class VideoPreviewLoader extends SwingWorker<BufferedImage, Void> {

    private final Path video;
    private final Consumer<BufferedImage> onLoaded;
    private final BiConsumer<Path, Exception> onFailed;

    VideoPreviewLoader(Path video, Consumer<BufferedImage> onLoaded, BiConsumer<Path, Exception> onFailed) {
        this.video = video;
        this.onLoaded = onLoaded;
        this.onFailed = onFailed;
    }

    @Override protected BufferedImage doInBackground() throws Exception {
        return VideoThumbnailExtractor.extractFrame(video);
    }

    @Override protected void done() {
        try {
            BufferedImage img = get();
            if (img != null) onLoaded.accept(img); else onFailed.accept(video, null);
        } catch (Exception e) {
            onFailed.accept(video, e);
        }
    }
}