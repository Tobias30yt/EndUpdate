package de.midnitesmp.endupdate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MithrylGiveCommandTest {
    @Test void defaultsToAnIngot() {
        assertEquals(new MithrylGiveCommand.Request("Alex", "ingot", 1), MithrylGiveCommand.parse(new String[]{"give", "Alex"}));
    }
    @Test void acceptsShortFormAndExplicitItems() {
        assertEquals(new MithrylGiveCommand.Request("Alex", "ingot", 16), MithrylGiveCommand.parse(new String[]{"Alex", "16"}));
        assertEquals(new MithrylGiveCommand.Request("Alex", "pickaxe", 2), MithrylGiveCommand.parse(new String[]{"GIVE", "Alex", "PICKAXE", "2"}));
    }
    @Test void rejectsMissingTargetAndInvalidAmounts() {
        for (String[] args : new String[][]{{}, {"give"}, {"Alex","0"}, {"Alex","-1"}, {"Alex","65"}, {"Alex","ingot","x"}, {"Alex","99999999999999"}, {"Alex","ingot","1","extra"}})
            assertThrows(IllegalArgumentException.class, () -> MithrylGiveCommand.parse(args));
    }
}
