package duplicatefinder.match;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileKindClassifierAudioTest {

    @Test
    void classifiesKnownAudioExtensionsAsAudio() {
        assertEquals(FileKind.AUDIO, FileKindClassifier.classify(Paths.get("song.MP3")));
        assertEquals(FileKind.AUDIO, FileKindClassifier.classify(Paths.get("song.m4a")));
        assertEquals(FileKind.AUDIO, FileKindClassifier.classify(Paths.get("song.flac")));
        assertEquals(FileKind.AUDIO, FileKindClassifier.classify(Paths.get("song.wav")));
    }

    @Test
    void doesNotClassifyVideoExtensionAsAudio() {
        assertEquals(FileKind.VIDEO, FileKindClassifier.classify(Paths.get("clip.mp4")));
    }
}