package duplicatefinder.gui;

import java.awt.Color;
import java.awt.Font;

/** Zentrale Farb- und Schrift-Konstanten, gemeinsam genutzt von allen GUI-Subpaketen. */
public final class UiTheme {

    private UiTheme() {}

    public static final Color BG = new Color(13, 17, 23);
    public static final Color SURFACE = new Color(22, 27, 34);
    public static final Color CARD = new Color(30, 37, 48);
    public static final Color BORDER = new Color(48, 54, 61);
    public static final Color ACCENT = new Color(88, 166, 255);
    public static final Color ACCENT_A = new Color(88, 166, 255, 45);
    public static final Color SUCCESS = new Color(63, 185, 80);
    public static final Color DANGER = new Color(248, 81, 73);
    public static final Color WARNING = new Color(210, 153, 34);
    public static final Color TEXT = new Color(230, 237, 243);
    public static final Color MUTED = new Color(110, 118, 129);

    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONT_UI = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
}
