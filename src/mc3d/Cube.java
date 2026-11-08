package mc3d;

import org.joml.Vector3f;

/**
 * Une instance de bloc dans le monde : une position + un type.
 * Leger : il ne possede NI mesh NI texture (ceux-ci sont partages et charges une seule fois).
 */
public class Cube {
    private final Vector3f position;
    private final BlockType blocktype;

    public Cube(float x, float y, float z, BlockType bt) {
        this.position = new Vector3f(x, y, z);
        this.blocktype = bt;
    }

    public Vector3f getPosition()   { return this.position; }
    public BlockType getBlockType() { return this.blocktype; }
}
