package mc3d;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;
import org.joml.Vector3i;

public class Maps {
	Map<ModelType, Mesh> model = new HashMap<>();
	Map<String, Texture> textures = new HashMap<>();
	Map<Vector3i, Chunk> worldMaps = new HashMap<>();
	boolean update = true; // true = la liste visible doit etre reconstruite
	FastNoiseLite noise;

	public Maps() {
		for (ModelType type : ModelType.values()) {
			model.put(type, ObjLoader.load("models/" + type + ".obj"));
		}
	}

	public void GenerateMaps() {
		this.noise = new FastNoiseLite();
		this.noise.SetSeed(12345);
		this.noise.SetFrequency(0.015f);
	}

	public void GenerateNewChunk(Vector3f playerPosition) {
		Vector3i test = worldPosToChunkPos(playerPosition);
		for (int x = -15; x <= 15; x++)
			for (int y = -3; y <= 3; y++)
				for (int z = -15; z <= 15; z++) {
					if (!this.worldMaps.containsKey(new Vector3i(test.x + x, test.y + y, test.z + z))) {
						this.worldMaps.put(new Vector3i(test.x + x, test.y + y, test.z + z),
								new Chunk(new Vector3i(test.x + x, test.y + y, test.z + z),
										GenerateChunk(this.noise, new Vector3i(test.x + x, test.y + y, test.z + z))));
						this.update = true;
					}
				}
	}

	public BlockType[][][] GenerateChunk(FastNoiseLite noise, Vector3i v3i) {
		BlockType[][][] blocks = new BlockType[Chunk.SIZE][Chunk.SIZE][Chunk.SIZE];
		for (int x = 0; x < Chunk.SIZE; x++)
			for (int y = 0; y < Chunk.SIZE; y++)
				for (int z = 0; z < Chunk.SIZE; z++) {
					int mondeX = v3i.x * Chunk.SIZE + x;
					int mondeZ = v3i.z * Chunk.SIZE + z;
					int mondeY = v3i.y * Chunk.SIZE + y;
					float n = noise.GetNoise(mondeX, mondeZ);
					int hauteur = (int) (20 + n * 10);
					if (mondeY < hauteur) {
						blocks[x][y][z] = BlockType.STONE;
					} else if (mondeY == hauteur) {
						blocks[x][y][z] = BlockType.DIRT;
					}

				}
		return blocks;
	}

	public void removeBlock(Vector3f v3f) {
		if (this.worldMaps.containsKey((worldPosToChunkPos(v3f)))) {
			Chunk chunk = this.worldMaps.get(worldPosToChunkPos(v3f));
			Vector3i blockPos = worldPosToChunkBlockPos(v3f);
			chunk.removeBlock(blockPos, this);
		}
		this.update = true; // le monde a change -> reconstruire la liste visible
	}

	public void placeBlock(Vector3f v3f, BlockType bt) {
		Vector3i ChunkPos = worldPosToChunkPos(v3f);
		if (!this.worldMaps.containsKey(ChunkPos)) {
			this.worldMaps.put(new Vector3i(ChunkPos.x, ChunkPos.y, ChunkPos.z),
					new Chunk(new Vector3i(ChunkPos.x, ChunkPos.y, ChunkPos.z),
							GenerateChunk(this.noise, new Vector3i(ChunkPos.x, ChunkPos.y, ChunkPos.z))));
		}
		Chunk chunk = this.worldMaps.get(ChunkPos);
		Vector3i blockPos = worldPosToChunkBlockPos(v3f);
		chunk.placeBlock(blockPos, bt, this);
		this.update = true; // le monde a change -> reconstruire la liste visible
	}

	public boolean isBlock(Vector3f v3f) {
		Vector3i ChunkPos = worldPosToChunkPos(v3f);
		if (this.worldMaps.containsKey(ChunkPos)) {
			Chunk chunk = this.worldMaps.get(ChunkPos);
			Vector3i blockPos = worldPosToChunkBlockPos(v3f);
			if (chunk.getBlockPos(blockPos) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Un bloc est visible s'il a AU MOINS UN voisin manquant (sinon il est
	 * enterre).
	 */
	private boolean estVisible(Cube block) {
		Vector3f p = block.getPosition();
		return !isBlock(new Vector3f(p.x, p.y + 1, p.z)) || !isBlock(new Vector3f(p.x, p.y - 1, p.z))
				|| !isBlock(new Vector3f(p.x + 1, p.y, p.z)) || !isBlock(new Vector3f(p.x - 1, p.y, p.z))
				|| !isBlock(new Vector3f(p.x, p.y, p.z - 1)) || !isBlock(new Vector3f(p.x, p.y, p.z + 1));
	}

	private Vector3i worldPosToChunkPos(Vector3f v3f) {
		return new Vector3i(Math.floorDiv(Math.round(v3f.x), Chunk.SIZE), Math.floorDiv(Math.round(v3f.y), Chunk.SIZE),
				Math.floorDiv(Math.round(v3f.z), Chunk.SIZE));
	}

	private Vector3i worldPosToChunkBlockPos(Vector3f v3f) {
		return new Vector3i(Math.floorMod(Math.round(v3f.x), Chunk.SIZE), Math.floorMod(Math.round(v3f.y), Chunk.SIZE),
				Math.floorMod(Math.round(v3f.z), Chunk.SIZE));
	}

	public void RenderWorld(ShaderProgram shader) {
		for (Chunk chunk : this.worldMaps.values()) {
			Mesh mesh = chunk.buildMesh(this);
			if (mesh != null) {
				Vector3i cp = chunk.getChunkPos();
				Matrix4f modelMatrix = new Matrix4f().translate(cp.x * Chunk.SIZE, cp.y * Chunk.SIZE,
						cp.z * Chunk.SIZE);
				shader.setUniform("model", modelMatrix);
				Texture t = textures.computeIfAbsent("textures/block/atlas.png", p -> new Texture(p));
				glActiveTexture(GL_TEXTURE0);
				t.bind();
				shader.setUniform("uTexture", 0);
				mesh.render();
			}

		}
	}

	public void cleanup() {
		for (Texture t : textures.values()) {
			t.cleanup();
		}
	}
}
