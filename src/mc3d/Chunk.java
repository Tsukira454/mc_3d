package mc3d;

import org.joml.Vector3i;

public class Chunk {
	private Vector3i chunkPosition;
	final int ChunkSIZE = 16;
	BlockType[][][] blocks = new BlockType[ChunkSIZE][ChunkSIZE][ChunkSIZE];

	public Chunk(Vector3i chunkPosition) {
		this.chunkPosition = chunkPosition;
		for (int x = 0; x < 16; x++)
			for (int z = 0; z < 16; z++) {
				blocks[x][0][z] = BlockType.DIRT;
			}
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
