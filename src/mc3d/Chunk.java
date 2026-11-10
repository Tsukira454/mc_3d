package mc3d;

import org.joml.Vector3i;

public class Chunk {
	private Vector3i chunkPosition;
	public static final int SIZE = 16;
	BlockType[][][] blocks;

	public Chunk(Vector3i chunkPosition, BlockType[][][] blocks) {
		this.chunkPosition = chunkPosition;
		this.blocks=blocks;
	}

	public void removeBlock(Vector3i position) {
		this.blocks[position.x][position.y][position.z] = null;
	}

	public void placeBlock(Vector3i position, BlockType bt) {
		this.blocks[position.x][position.y][position.z] = bt;
	}

	public Vector3i getChunkPos() {
		return this.chunkPosition;
	}

	public BlockType getBlockPos(Vector3i v3i) {
		return blocks[v3i.x][v3i.y][v3i.z];
	}
}
