package lab3.ui.run;

import lab3.core.SimulationConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunSettingsTest {

    @Test
    void defaultsAreScenarioOneWithTheDefaultSeed() {
        RunSettings settings = RunSettings.defaults();

        assertEquals(Scenario.NO_IMMUNITY, settings.scenario());
        assertEquals(42, settings.seed());
        assertEquals(SimulationConfig.noImmunity(), settings.config());
    }

    @Test
    void scenarioOneIgnoresTheImmuneShare() {
        RunSettings settings = new RunSettings(Scenario.NO_IMMUNITY, 0.8, 1);

        assertEquals(0.0, settings.config().immuneProbability(), 0);
    }

    @Test
    void scenarioTwoUsesTheImmuneShare() {
        RunSettings settings = new RunSettings(Scenario.WITH_IMMUNITY, 0.55, 1);

        assertEquals(0.55, settings.config().immuneProbability(), 0);
        assertEquals(SimulationConfig.withImmunity(0.55), settings.config());
    }

    @Test
    void immuneShareMustBeAProbability() {
        assertThrows(IllegalArgumentException.class, () -> new RunSettings(Scenario.WITH_IMMUNITY, -0.1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RunSettings(Scenario.WITH_IMMUNITY, 1.1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RunSettings(Scenario.WITH_IMMUNITY, Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new RunSettings(null, 0.3, 1));
        new RunSettings(Scenario.WITH_IMMUNITY, 0, 1);
        new RunSettings(Scenario.WITH_IMMUNITY, 1, 1);
    }

    @Test
    void parsesSeeds() {
        assertEquals(42, RunSettings.parseSeed("42"));
        assertEquals(-7, RunSettings.parseSeed(" -7 "));
        assertEquals(Long.MAX_VALUE, RunSettings.parseSeed("9223372036854775807"));
        assertEquals(0, RunSettings.parseSeed("0"));
    }

    @Test
    void badSeedsGiveAMessageForTheUser() {
        for (String bad : new String[] {"", "   ", "abc", "4.5", "12 34", "99999999999999999999", null}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> RunSettings.parseSeed(bad));
            assertFalse(e.getMessage().isBlank());
            assertTrue(e.getMessage().contains("Ziarno") || e.getMessage().contains("ziarno"), e.getMessage());
        }
    }

    @Test
    void emptySeedGetsItsOwnMessage() {
        for (String empty : new String[] {"", "   ", null}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> RunSettings.parseSeed(empty));
            assertTrue(e.getMessage().startsWith("Podaj"), e.getMessage());
        }
        IllegalArgumentException notNumber = assertThrows(IllegalArgumentException.class, () -> RunSettings.parseSeed("abc"));
        assertTrue(notNumber.getMessage().contains("abc"), notNumber.getMessage());
    }

    @Test
    void scenariosHaveDifferentTitles() {
        assertFalse(Scenario.NO_IMMUNITY.title().equals(Scenario.WITH_IMMUNITY.title()));
        assertTrue(Scenario.NO_IMMUNITY.title().contains("1"));
        assertTrue(Scenario.WITH_IMMUNITY.title().contains("2"));
    }
}
