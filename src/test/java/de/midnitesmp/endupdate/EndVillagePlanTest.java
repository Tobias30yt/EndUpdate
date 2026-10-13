package de.midnitesmp.endupdate;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EndVillagePlanTest {
    @Test void allHouseEntrancesReachCentralSquare() {
        var blocks=EndVillagePlan.create().blocks();
        Set<EndVillagePlan.Pos> seen=new HashSet<>();Deque<EndVillagePlan.Pos> open=new ArrayDeque<>();
        open.add(new EndVillagePlan.Pos(0,0,1));
        while(!open.isEmpty()){
            var p=open.removeFirst();if(!seen.add(p))continue;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                var next=new EndVillagePlan.Pos(p.x()+d[0],0,p.z()+d[1]);
                var body=blocks.get(new EndVillagePlan.Pos(next.x(),1,next.z()));
                var head=blocks.get(new EndVillagePlan.Pos(next.x(),2,next.z()));
                if(blocks.containsKey(next)&&(body==null||body.wood().equals("end_door"))&&(head==null||head.wood().equals("end_door"))&&!seen.contains(next))open.add(next);
            }
        }
        for(int x:new int[]{-8,8})for(int z:new int[]{-8,8})assertTrue(seen.contains(new EndVillagePlan.Pos(x,0,z+(z<0?3:-3))));
    }
    @Test void candidateRemainsInsideRegionIncludingNegativeCoordinates() {
        for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++){
            var c=EndVillages.candidate(1234,x,z,16);
            assertEquals(x,Math.floorDiv(c.x(),16));assertEquals(z,Math.floorDiv(c.z(),16));
            assertTrue(Math.floorMod(c.x(),16)>=2&&Math.floorMod(c.x(),16)<=13);
            assertEquals(c,EndVillages.candidate(1234,x,z,16));
        }
    }
}
