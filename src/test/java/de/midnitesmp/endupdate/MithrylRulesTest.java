package de.midnitesmp.endupdate;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MithrylRulesTest {
    @Test void protectsCentralIslandAndHandlesWorldBorder(){
        assertFalse(MithrylRules.outerIsland(0,0,1024));assertFalse(MithrylRules.outerIsland(1023,0,1024));
        assertTrue(MithrylRules.outerIsland(1024,0,1024));assertTrue(MithrylRules.outerIsland(-30_000_000,30_000_000,1024));
    }
    @Test void fortuneUsesVanillaOreDistribution(){
        Random random=new Random(42);int total=0;Set<Integer> drops=new HashSet<>();
        for(int i=0;i<100000;i++){int amount=MithrylRules.fortuneDrops(3,random);assertTrue(amount>=1&&amount<=4);drops.add(amount);total+=amount;}
        assertEquals(Set.of(1,2,3,4),drops);assertEquals(2.2,total/100000.0,.02);
        assertEquals(1,MithrylRules.fortuneDrops(0,random));assertEquals(1,MithrylRules.fortuneDrops(-1,random));
    }
    @Test void exactArmorThresholds(){assertEquals(-1,MithrylRules.speedAmplifier(0));for(int n=1;n<4;n++)assertEquals(0,MithrylRules.speedAmplifier(n));assertEquals(1,MithrylRules.speedAmplifier(4));}
    @Test void chunkCoordinatesHaveNoCollisions(){Set<Integer> values=new HashSet<>();for(int y=-64;y<320;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)assertTrue(values.add(MithrylRules.encode(x,y,z,-64)));assertEquals(MithrylRules.encode(15,100,15,-64),MithrylRules.encode(-1,100,-1,-64));}
}
