package de.midnitesmp.endupdate;

import java.util.*;

/** Relative block blueprint, shared by generation and the offline structure preview. */
public final class EndVillagePlan {
    public record Pos(int x,int y,int z) {}
    public record Cell(String material,String wood,String states) {}
    private final Map<Pos,Cell> blocks=new LinkedHashMap<>();
    public Map<Pos,Cell> blocks(){return Collections.unmodifiableMap(blocks);}
    private void put(int x,int y,int z,String material){blocks.put(new Pos(x,y,z),new Cell(material,"",""));}
    private void wood(int x,int y,int z,String kind){wood(x,y,z,kind,"");}
    private void wood(int x,int y,int z,String kind,String states){blocks.put(new Pos(x,y,z),new Cell("", "end_"+kind,states));}
    public static EndVillagePlan create() {
        EndVillagePlan p=new EndVillagePlan();
        // Three-block-wide raised boardwalks, with low curb posts and End-rod lamps.
        for(int a=-13;a<=13;a++)for(int b=-1;b<=1;b++){p.wood(a,0,b,"planks");p.wood(b,0,a,"planks");}
        for(int x:new int[]{-8,8})for(int z:new int[]{-8,8}) {
            p.house(x,z,z<0?1:-1);
            for(int dz=0;dz<=4;dz++)for(int dx=-1;dx<=1;dx++)p.wood(x+dx,0,(z<0?-1:1)*dz,"planks");
        }
        // Open pavilion: no bed/respawn anchor that could explode in the End.
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)p.put(x,0,z,"PURPUR_BLOCK");
        for(int x:new int[]{-2,2})for(int z:new int[]{-2,2})for(int y=1;y<=4;y++)p.wood(x,y,z,"log");
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)p.wood(x,5,z,"slab");
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)p.put(x,6,z,"PURPUR_BLOCK");
        p.put(0,7,0,"END_ROD");p.put(0,1,0,"CHISELED_QUARTZ_BLOCK");p.put(0,2,0,"AMETHYST_CLUSTER");
        for(int x:new int[]{-3,3})for(int z:new int[]{-12,12}){p.wood(x,0,z,"planks");p.wood(x,1,z,"fence");p.wood(x,2,z,"fence");p.put(x,3,z,"END_ROD");}
        return p;
    }
    private void house(int cx,int cz,int front) {
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++) {
            wood(cx+x,0,cz+z,"planks");
            for(int y=1;y<=3;y++)if(Math.abs(x)==3||Math.abs(z)==3)wood(cx+x,y,cz+z,Math.abs(x)==3&&Math.abs(z)==3?"log":"stripped_wood");
        }
        // Tall, inset windows and a centered doorway facing the paths.
        for(int y=2;y<=3;y++)for(int d=-1;d<=1;d++) {
            put(cx-3,y,cz+d,"PURPLE_STAINED_GLASS");put(cx+3,y,cz+d,"PURPLE_STAINED_GLASS");
            put(cx+d,y,cz-3*front,"PURPLE_STAINED_GLASS");
        }
        String face=front>0?"south":"north";
        wood(cx,1,cz+3*front,"door","facing="+face+",half=lower");
        wood(cx,2,cz+3*front,"door","facing="+face+",half=upper");
        // End-City-inspired stepped eaves with a small raised cap.
        for(int tier=0;tier<3;tier++){int r=4-tier;for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)
            wood(cx+x,4+tier,cz+z,Math.abs(x)==r||Math.abs(z)==r?"slab":"planks");}
        put(cx,7,cz,"END_ROD");
        put(cx-2,1,cz-2*front,"CHEST");put(cx+2,1,cz-2*front,"CRAFTING_TABLE");
        wood(cx+2,1,cz,"stairs","facing=west");wood(cx-2,1,cz,"stairs","facing=east");
        put(cx,1,cz-2*front,"PURPUR_BLOCK");
    }
}
