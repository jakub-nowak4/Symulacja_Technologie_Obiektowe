package lab3.ui.draw;

/**
 * Converts positions in the area (metres) to positions on a canvas (pixels). The area keeps its
 * proportions and is centred in the canvas, with a margin around it. The origin is the top left
 * corner and the y axis points down, like on the screen, so the picture is the area seen from above.
 *
 * <p>Contains no JavaFX, so it can be tested without a window.
 */
public final class WorldTransform {
    private final double scale;
    private final double offsetX;
    private final double offsetY;
    private final double areaWidth;
    private final double areaHeight;

    /**
     * @param areaWidth    width of the area in metres, positive
     * @param areaHeight   height of the area in metres, positive
     * @param canvasWidth  width of the canvas in pixels, not negative
     * @param canvasHeight height of the canvas in pixels, not negative
     * @param margin       free space around the area in pixels, not negative
     */
    public WorldTransform(double areaWidth, double areaHeight, double canvasWidth, double canvasHeight, double margin) {
        if (!(areaWidth > 0) || !(areaHeight > 0) || Double.isInfinite(areaWidth) || Double.isInfinite(areaHeight)) {
            throw new IllegalArgumentException("area size must be positive and finite: " + areaWidth + " x " + areaHeight);
        }
        if (!(canvasWidth >= 0) || !(canvasHeight >= 0) || !(margin >= 0)) {
            throw new IllegalArgumentException("canvas size and margin must not be negative");
        }
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;
        double freeWidth = Math.max(0, canvasWidth - 2 * margin);
        double freeHeight = Math.max(0, canvasHeight - 2 * margin);
        this.scale = Math.min(freeWidth / areaWidth, freeHeight / areaHeight);
        this.offsetX = (canvasWidth - areaWidth * scale) / 2;
        this.offsetY = (canvasHeight - areaHeight * scale) / 2;
    }

    /** Pixels per metre; zero when the canvas is too small to show anything. */
    public double scale() {
        return scale;
    }

    public double toPixelX(double metresX) {
        return offsetX + metresX * scale;
    }

    public double toPixelY(double metresY) {
        return offsetY + metresY * scale;
    }

    /** Converts a length (for example a radius) from metres to pixels. */
    public double toPixels(double metres) {
        return metres * scale;
    }

    /** Left edge of the area on the canvas. */
    public double areaLeft() {
        return offsetX;
    }

    /** Top edge of the area on the canvas. */
    public double areaTop() {
        return offsetY;
    }

    public double areaPixelWidth() {
        return areaWidth * scale;
    }

    public double areaPixelHeight() {
        return areaHeight * scale;
    }
}
