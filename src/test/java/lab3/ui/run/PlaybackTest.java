package lab3.ui.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackTest {
    private static final long MS = 1_000_000L;

    private static int stepsInOneSecond(Playback playback, long frameNanos) {
        int total = 0;
        for (long t = 0; t < 1_000_000_000L; t += frameNanos) {
            total += playback.stepsFor(frameNanos);
        }
        return total;
    }

    @Test
    void doesNothingUntilStarted() {
        Playback playback = new Playback();

        assertFalse(playback.isRunning());
        assertEquals(0, playback.stepsFor(100 * MS));
    }

    @Test
    void atSpeedOneItRunsTwentyFiveStepsPerSecond() {
        Playback playback = new Playback();
        playback.start();

        assertEquals(25, stepsInOneSecond(playback, 20 * MS));    // 50 frames per second
    }

    @Test
    void frameRateDoesNotChangeTheNumberOfSteps() {
        for (long frame : new long[] {1 * MS, 8 * MS, 16_666_667L, 33 * MS, 100 * MS}) {
            Playback playback = new Playback();
            playback.start();

            int steps = 0;
            long total = 10_000 * MS;   // ten seconds
            for (long t = 0; t < total; t += frame) {
                steps += playback.stepsFor(frame);
            }

            assertTrue(Math.abs(steps - 250) <= 3, "frame " + frame + " ns gave " + steps + " steps");
        }
    }

    @Test
    void speedMultipliesTheNumberOfSteps() {
        for (double speed : Playback.SPEEDS) {
            Playback playback = new Playback();
            playback.setSpeed(speed);
            playback.start();

            int steps = stepsInOneSecond(playback, 20 * MS);

            assertEquals(25 * speed, steps, 1.01, "speed " + speed);
        }
    }

    @Test
    void partOfAStepIsCarriedToTheNextFrame() {
        Playback playback = new Playback();
        playback.start();

        // every 30 ms frame is worth 0.75 of a step
        assertEquals(0, playback.stepsFor(30 * MS));   // 0.75
        assertEquals(1, playback.stepsFor(30 * MS));   // 1.50 -> one step, 0.50 left
        assertEquals(1, playback.stepsFor(30 * MS));   // 1.25 -> one step, 0.25 left
        assertEquals(1, playback.stepsFor(30 * MS));   // 1.00 -> one step, nothing left
        assertEquals(0, playback.stepsFor(30 * MS));   // 0.75 again
    }

    @Test
    void pauseStopsTheStepsAndDropsAPartialStep() {
        Playback playback = new Playback();
        playback.start();
        playback.stepsFor(30 * MS);

        playback.pause();
        assertEquals(0, playback.stepsFor(1000 * MS));

        playback.start();
        assertEquals(0, playback.stepsFor(30 * MS));   // starts from zero, not from 0.75
    }

    @Test
    void aLongPauseBetweenFramesIsCutShort() {
        Playback playback = new Playback();
        playback.start();

        int steps = playback.stepsFor(60_000 * MS);    // the window was blocked for a minute

        assertEquals(6, steps);                        // 0.25 s at 25 steps per second
    }

    @Test
    void neverRunsMoreThanTheLimitInOneFrame() {
        Playback playback = new Playback();
        playback.setSpeed(1000);                        // not offered to users, but must be safe
        playback.start();

        assertEquals(Playback.MAX_STEPS_PER_FRAME, playback.stepsFor(200 * MS));
    }

    @Test
    void zeroOrNegativeElapsedTimeGivesNoSteps() {
        Playback playback = new Playback();
        playback.start();

        assertEquals(0, playback.stepsFor(0));
        assertEquals(0, playback.stepsFor(-5 * MS));
    }

    @Test
    void speedCanBeChangedWhileRunning() {
        Playback playback = new Playback();
        playback.start();
        assertEquals(25, stepsInOneSecond(playback, 20 * MS));

        playback.setSpeed(2.0);

        assertEquals(50, stepsInOneSecond(playback, 20 * MS));
        assertEquals(2.0, playback.speed(), 0);
    }

    @Test
    void rejectsWrongSpeeds() {
        Playback playback = new Playback();

        assertThrows(IllegalArgumentException.class, () -> playback.setSpeed(0));
        assertThrows(IllegalArgumentException.class, () -> playback.setSpeed(-1));
        assertThrows(IllegalArgumentException.class, () -> playback.setSpeed(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> playback.setSpeed(Double.POSITIVE_INFINITY));
        assertEquals(1.0, playback.speed(), 0);
    }

    @Test
    void offeredSpeedsIncludeRealTimeAndAreSortedAndPositive() {
        assertTrue(Playback.SPEEDS.contains(1.0));
        for (int i = 0; i < Playback.SPEEDS.size(); i++) {
            assertTrue(Playback.SPEEDS.get(i) > 0);
            if (i > 0) {
                assertTrue(Playback.SPEEDS.get(i) > Playback.SPEEDS.get(i - 1));
            }
        }
    }

    @Test
    void labelsShowWholeNumbersWithoutDecimals() {
        assertEquals("1×", Playback.label(1.0));
        assertEquals("25×", Playback.label(25.0));
        assertEquals("0.25×", Playback.label(0.25));
        assertEquals("0.5×", Playback.label(0.5));
    }
}
