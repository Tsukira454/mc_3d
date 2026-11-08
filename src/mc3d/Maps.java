package mc3d;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

public class Maps {
	Map<ModelType, Mesh> model = new HashMap<>();
	Map<String, Texture> textures = new HashMap<>();
	Map<Vector3f, Cube> worldMaps = new HashMap<>();
	boolean update = true; // true = la liste visible doit etre reconstruite
	List<Cube> renderBlockList = new ArrayList<>(); // les blocs reellement visibles (cache)

	public Maps() {
		for (ModelType type : ModelType.values()) {
			model.put(type, ObjLoader.load("models/" + type + ".obj"));
		}
	}

	public void GenerateMaps() {
		List<BlockType> naturalBlock = new ArrayList<BlockType>();
		for (BlockType blockType : BlockType.values()) {
			if (blockType.getBlockSpawn() == BlockSpawn.NATURAL) {
				naturalBlock.add(blockType);
			}
		}
		for (int y = 0; y > -5; y--) {
			for (int x = -15; x < 15; x++) {
				for (int z = -15; z < 15; z++) {
					this.worldMaps.put(new Vector3f(x, y, z),
							new Cube(x, y, z, naturalBlock.get((int) (Math.random() * naturalBlock.size()))));
				}
			}
		}
		this.worldMaps.put(new Vector3f(0, 5, 0), new Cube(0, 5, 0, BlockType.STAIRS));
	}

	public void removeBlock(Vector3f v3) {
		this.worldMaps.remove(v3);
		this.update = true; // le monde a change -> reconstruire la liste visible
	}

	public void placeBlock(Vector3f v3, BlockType bt) {
		this.worldMaps.put(new Vector3f(v3), new Cube(v3.x, v3.y, v3.z, bt));
		this.update = true; // le monde a change -> reconstruire la liste visible
	}
	
	public boolean isBlock(Vector3f v3) {
		return this.worldMaps.containsKey(v3);
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
		for (Cube block : this.worldMaps.values()) {
			if (estVisible(block)) {
				this.renderBlockList.add(block);
			}
		}
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
