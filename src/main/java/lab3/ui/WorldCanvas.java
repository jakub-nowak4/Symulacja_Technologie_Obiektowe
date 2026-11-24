package lab3.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lab3.core.SimulationConfig;
import lab3.core.view.PersonView;
import lab3.core.view.WorldView;
import lab3.ui.draw.WorldTransform;

/**
 * Draws a {@link WorldView}: the area as a frame and every person as a dot in the colour of their
 * health category. It only draws what it is given; it knows nothing about the simulation.
 */
public final class WorldCanvas extends Canvas {
    private static final double MARGIN = 20;
    private static final double PERSON_RADIUS_PX = 4.5;
    private static final Color BACKGROUND = Color.rgb(0xF7, 0xF7, 0xF5);
    private static final Color AREA_FILL = Color.WHITE;
    private static final Color AREA_BORDER = Color.rgb(0x44, 0x44, 0x44);

    private WorldView view;
    private boolean showContactRadius;

    /** Shows the given world and draws it again. */
    public void setView(WorldView view) {
        this.view = view;
        redraw();
    }

    /** Switches the circles of {@link SimulationConfig#CONTACT_RADIUS} around infected people on or off. */
    public void setShowContactRadius(boolean show) {
        this.showContactRadius = show;
        redraw();
    }

    /** Changes the size of the canvas and draws again. */
    public void resizeTo(double width, double height) {
        setWidth(Math.max(0, width));
        setHeight(Math.max(0, height));
        redraw();
    }

    private void redraw() {
        GraphicsContext g = getGraphicsContext2D();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (view == null) {
            return;
        }

        WorldTransform t = new WorldTransform(view.areaWidth(), view.areaHeight(), getWidth(), getHeight(), MARGIN);
        g.setFill(AREA_FILL);
        g.fillRect(t.areaLeft(), t.areaTop(), t.areaPixelWidth(), t.areaPixelHeight());
        g.setStroke(AREA_BORDER);
        g.setLineWidth(2);
        g.strokeRect(t.areaLeft(), t.areaTop(), t.areaPixelWidth(), t.areaPixelHeight());

        if (showContactRadius) {
            drawContactRadii(g, t);
        }
        for (PersonView person : view.persons()) {
            g.setFill(Colors.of(person.kind()));
            double x = t.toPixelX(person.x());
            double y = t.toPixelY(person.y());
            g.fillOval(x - PERSON_RADIUS_PX, y - PERSON_RADIUS_PX, 2 * PERSON_RADIUS_PX, 2 * PERSON_RADIUS_PX);
        }
    }

    private void drawContactRadii(GraphicsContext g, WorldTransform t) {
        double radius = t.toPixels(SimulationConfig.CONTACT_RADIUS);
        g.setLineWidth(1);
        for (PersonView person : view.persons()) {
            if (!person.kind().isInfected()) {
                continue;
            }
            g.setStroke(Colors.of(person.kind(), 0.45));
            g.strokeOval(t.toPixelX(person.x()) - radius, t.toPixelY(person.y()) - radius, 2 * radius, 2 * radius);
        }
    }
}
