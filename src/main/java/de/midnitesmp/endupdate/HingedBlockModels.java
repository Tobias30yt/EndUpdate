package de.midnitesmp.endupdate;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Vanilla blockstate rotations, applied around the centre of an ItemDisplay model. */
public final class HingedBlockModels {
    private HingedBlockModels() {}
    public record State(String model, int x, int y) {
        public Transformation transformation() {
            // Minecraft blockstate angles are clockwise; JOML positive angles are counterclockwise.
            Quaternionf rotation = new Quaternionf().rotationY((float)Math.toRadians(-y))
                .rotateX((float)Math.toRadians(-x));
            return new Transformation(new Vector3f(), rotation, new Vector3f(1), new Quaternionf());
        }
    }
    public static State of(BlockData data) {
        if (data instanceof Door door) return door(door.getFacing(),door.getHalf()==Bisected.Half.TOP,door.getHinge()==Door.Hinge.RIGHT,door.isOpen());
        if (data instanceof TrapDoor trapdoor) return trapdoor(trapdoor.getFacing(),trapdoor.getHalf()==Bisected.Half.TOP,trapdoor.isOpen());
        return null;
    }
    public static State door(BlockFace facing, boolean top, boolean right, boolean open) {
        int yaw = switch(facing){case EAST->0;case SOUTH->90;case WEST->180;case NORTH->270;default->throw new IllegalArgumentException("Horizontal facing required");};
        if (open) yaw += right ? 270 : 90;
        return new State("end_door_display_"+(top?"top":"bottom")+"_"+(right?"right":"left")+(open?"_open":""),0,yaw%360);
    }
    public static State trapdoor(BlockFace facing, boolean top, boolean open) {
        int yaw = switch(facing){case NORTH->0;case EAST->90;case SOUTH->180;case WEST->270;default->throw new IllegalArgumentException("Horizontal facing required");};
        if (open && top) yaw += 180;
        return new State("end_trapdoor_display_"+(open?"open":top?"top":"bottom"),open&&top?180:0,yaw%360);
    }
}
