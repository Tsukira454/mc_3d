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
	List<Cube> renderBlockList = new ArrayList<>(); // les blocs reellement visibles (cache)

	public Maps() {
		for (ModelType type : ModelType.values()) {
			model.put(type, ObjLoader.load("models/" + type + ".obj"));
		}
	}

	/*
	 * public void GenerateMaps() { List<BlockType> naturalBlock = new
	 * ArrayList<BlockType>(); for (BlockType blockType : BlockType.values()) { if
	 * (blockType.getBlockSpawn() == BlockSpawn.NATURAL) {
	 * naturalBlock.add(blockType); } } for (int y = 0; y > -5; y--) { for (int x =
	 * -15; x < 15; x++) { for (int z = -15; z < 15; z++) { this.worldMaps.put(new
	 * Vector3f(x, y, z), new Cube(x, y, z, naturalBlock.get((int) (Math.random() *
	 * naturalBlock.size())))); } } } this.worldMaps.put(new Vector3f(0, 5, 0), new
	 * Cube(0, 5, 0, BlockType.STAIRS)); }
	 */

	public void GenerateMaps() {
		for (int x = -1; x < 1; x++)
			for (int z = -1; z < 1; z++)
				this.worldMaps.put(new Vector3i(x, 0, z), new Chunk(new Vector3i(x, 0, z)));
	}

	public void GenerateChunk(Vector3f playerPosition) {
		Vector3i test = worldPosToChunkPos(playerPosition);
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++) {
				if (!this.worldMaps.containsKey(new Vector3i(test.x + x, 0, test.z + z))) {
					this.worldMaps.put(new Vector3i(test.x + x, 0, test.z + z),
							new Chunk(new Vector3i(test.x + x, 0, test.z + z)));
					this.update = true;
				}
			}
	}

	public void removeBlock(Vector3f v3f) {
		if (this.worldMaps.containsKey((worldPosToChunkPos(v3f)))) {
			Chunk chunk = this.worldMaps.get(worldPosToChunkPos(v3f));
			Vector3i blockPos = worldPosToChunkBlockPos(v3f);
			chunk.removeBlock(blockPos);
		}
		this.update = true; // le monde a change -> reconstruire la liste visible
	}

	public void placeBlock(Vector3f v3f, BlockType bt) {
		if (this.worldMaps.containsKey((worldPosToChunkPos(v3f)))) {
			Chunk chunk = this.worldMaps.get(worldPosToChunkPos(v3f));
			Vector3i blockPos = worldPosToChunkBlockPos(v3f);
			chunk.placeBlock(blockPos, bt);
		}
		this.update = true; // le monde a change -> reconstruire la liste visible
	}

	public boolean isBlock(Vector3f v3) {
		if (this.worldMaps.containsKey(worldPosToChunkPos(v3))) {
			Chunk chunk = this.worldMaps.get(worldPosToChunkPos(v3));
			Vector3i blockPos = worldPosToChunkBlockPos(v3);
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

	/**
	 * Reconstruit la liste des blocs visibles (appele seulement quand le monde
	 * change).
	 */
	private void rebuildRenderList() {
		this.renderBlockList.clear();
		for (Chunk Chunk : this.worldMaps.values()) {
			for (int x = 0; x < 16; x++)
				for (int y = 0; y < 16; y++)
					for (int z = 0; z < 16; z++) {
						if (Chunk.getBlockPos(new Vector3i(x, y, z)) != null) {
							Vector3i ChunkPos = Chunk.getChunkPos();
							Cube block = new Cube((ChunkPos.x * 16) + x, (ChunkPos.y * 16) + y, (ChunkPos.z * 16) + z,
									Chunk.getBlockPos(new Vector3i(x, y, z)));
							if (estVisible(block)) {
								this.renderBlockList.add(block);
							}
						}
					}
		}
	}

	private Vector3i worldPosToChunkPos(Vector3f v3f) {
		return new Vector3i(Math.floorDiv(Math.round(v3f.x), 16), Math.floorDiv(Math.round(v3f.y), 16),
				Math.floorDiv(Math.round(v3f.z), 16));
	}

	private Vector3i worldPosToChunkBlockPos(Vector3f v3f) {
		return new Vector3i(Math.floorMod(Math.round(v3f.x), 16), Math.floorMod(Math.round(v3f.y), 16),
				Math.floorMod(Math.round(v3f.z), 16));
	}

	public void RenderWorld(ShaderProgram shader) {
		if (this.update) {
			rebuildRenderList();
			this.update = false; // calcule une fois, puis on reutilise le cache
		}
		for (Cube block : this.renderBlockList) {
			Texture t = textures.computeIfAbsent(block.getBlockType().getTexturePath(), p -> new Texture(p));
			glActiveTexture(GL_TEXTURE0);
			t.bind();
			shader.setUniform("uTexture", 0);

			Matrix4f modelMatrix = new Matrix4f().translate(block.getPosition());
			shader.setUniform("model", modelMatrix);
			Mesh mesh = model.get(block.getBlockType().getModel());
			mesh.render();
		}
	}

	public void cleanup() {
		for (Texture t : textures.values()) {
			t.cleanup();
		}
	}
}
