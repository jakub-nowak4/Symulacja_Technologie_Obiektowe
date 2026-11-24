package lab3.core.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthKindTest {

    @Test
    void onlyBothInfectedKindsAreInfected() {
        assertTrue(HealthKind.INFECTED_ASYMPTOMATIC.isInfected());
        assertTrue(HealthKind.INFECTED_SYMPTOMATIC.isInfected());
        assertFalse(HealthKind.HEALTHY.isInfected());
        assertFalse(HealthKind.IMMUNE.isInfected());
    }
}
