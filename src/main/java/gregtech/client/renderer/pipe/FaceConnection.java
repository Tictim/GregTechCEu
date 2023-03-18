package gregtech.client.renderer.pipe;

import net.minecraft.util.EnumFacing;

/**
 * Connection state of a face, in 2-dimensional plane (left/right/up/down)
 */
public class FaceConnection {

    public static final byte NO_CONNECTIONS = 0;
    public static final byte ALL_CONNECTIONS = 0b1111;

    public static byte of(boolean connectedToLeft, boolean connectedToRight, boolean connectedToUp, boolean connectedToDown){
        byte flags = 0;
        if(connectedToLeft) flags |= 1;
        if(connectedToRight) flags |= 2;
        if(connectedToUp) flags |= 4;
        if(connectedToDown) flags |= 8;
        return flags;
    }

    public static byte forFace(int connections, EnumFacing side){
        boolean connectedToLeft;
        boolean connectedToRight;
        boolean connectedToUp;
        boolean connectedToDown;

        switch (side) {
            case DOWN:
                connectedToLeft = isConnected(connections, EnumFacing.WEST);
                connectedToRight = isConnected(connections, EnumFacing.EAST);
                connectedToUp = isConnected(connections, EnumFacing.SOUTH);
                connectedToDown = isConnected(connections, EnumFacing.NORTH);
                break;
            case UP:
                connectedToLeft = isConnected(connections, EnumFacing.WEST);
                connectedToRight = isConnected(connections, EnumFacing.EAST);
                connectedToUp = isConnected(connections, EnumFacing.NORTH);
                connectedToDown = isConnected(connections, EnumFacing.SOUTH);
                break;
            case NORTH:
                connectedToLeft = isConnected(connections, EnumFacing.EAST);
                connectedToRight = isConnected(connections, EnumFacing.WEST);
                connectedToUp = isConnected(connections, EnumFacing.UP);
                connectedToDown = isConnected(connections, EnumFacing.DOWN);
                break;
            case SOUTH:
                connectedToLeft = isConnected(connections, EnumFacing.WEST);
                connectedToRight = isConnected(connections, EnumFacing.EAST);
                connectedToUp = isConnected(connections, EnumFacing.UP);
                connectedToDown = isConnected(connections, EnumFacing.DOWN);
                break;
            case WEST:
                connectedToLeft = isConnected(connections, EnumFacing.NORTH);
                connectedToRight = isConnected(connections, EnumFacing.SOUTH);
                connectedToUp = isConnected(connections, EnumFacing.UP);
                connectedToDown = isConnected(connections, EnumFacing.DOWN);
                break;
            case EAST:
                connectedToLeft = isConnected(connections, EnumFacing.SOUTH);
                connectedToRight = isConnected(connections, EnumFacing.NORTH);
                connectedToUp = isConnected(connections, EnumFacing.UP);
                connectedToDown = isConnected(connections, EnumFacing.DOWN);
                break;
            default:
                throw new IllegalStateException("Unreachable");
        }
        return of(connectedToLeft, connectedToRight, connectedToUp, connectedToDown);
    }

    public static boolean connectedToLeft(byte flags){
        return (flags & 1) != 0;
    }

    public static boolean connectedToRight(byte flags){
        return (flags & 2) != 0;
    }

    public static boolean connectedToUp(byte flags){
        return (flags & 4) != 0;
    }

    public static boolean connectedToDown(byte flags) {
        return (flags & 8) != 0;
    }

    public static boolean connectedToAnyXPlane(byte flags){
        return (flags & (1|2)) != 0;
    }

    public static boolean connectedToAnyYPlane(byte flags) {
        return (flags & (4|8)) != 0;
    }

    private static boolean isConnected(int connections, EnumFacing side) {
        return (connections & 1 << side.getIndex()) != 0;
    }
}
