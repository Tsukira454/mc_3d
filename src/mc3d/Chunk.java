package mc3d;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import org.joml.Vector3i;

public class Chunk {
	private Vector3i chunkPosition;
	public static final int SIZE = 16;
	BlockType[][][] blocks;
	
	
	private boolean meshIsUpdate=false;
	private Mesh chunkMesh;
	static final float H = 0.5f;
	static final float[][] FACE_DESSUS = { { -H, H, H }, { H, H, H }, { H, H, -H }, { -H, H, -H } };
	static final float[][] FACE_DESSOUS = { { -H, -H, -H }, { H, -H, -H }, { H, -H, H }, { -H, -H, H } };
	static final float[][] FACE_SUD = { { -H, -H, H }, { H, -H, H }, { H, H, H }, { -H, H, H } };     // +Z
	static final float[][] FACE_NORD = { { H, -H, -H }, { -H, -H, -H }, { -H, H, -H }, { H, H, -H } }; // -Z
	static final float[][] FACE_EST = { { H, -H, H }, { H, -H, -H }, { H, H, -H }, { H, H, H } };     // +X
	static final float[][] FACE_OUEST = { { -H, -H, -H }, { -H, -H, H }, { -H, H, H }, { -H, H, -H } }; // -X

	public Chunk(Vector3i chunkPosition, BlockType[][][] blocks) {
		this.chunkPosition = chunkPosition;
		this.blocks=blocks;
	}

	public void removeBlock(Vector3i position, Maps maps) {
		this.blocks[position.x][position.y][position.z] = null;
		this.updateOtherChunk(position, maps);
		this.meshIsUpdate=false;
	}

	public void placeBlock(Vector3i position, BlockType bt, Maps maps) {
		this.blocks[position.x][position.y][position.z] = bt;
		this.updateOtherChunk(position, maps);
		this.meshIsUpdate=false;
	}

	public Vector3i getChunkPos() {
		return this.chunkPosition;
	}

	public BlockType getBlockPos(Vector3i v3i) {
		return blocks[v3i.x][v3i.y][v3i.z];
	}
	
	// :)
	public void youWantMeToUpdate() {
		this.meshIsUpdate = false;
	}
	
	/**
	 * Si le bloc modifie est sur un BORD du chunk, le chunk voisin de ce cote-la
	 * doit aussi refaire son mesh (sa face collee a ce bloc vient d'apparaitre ou
	 * de disparaitre).
	 */
	private void updateOtherChunk(Vector3i position, Maps maps) {
		if (position.x == 0)        updateVoisin(maps, -1, 0, 0); // bord gauche  -> chunk x-1
		if (position.x == SIZE - 1) updateVoisin(maps, +1, 0, 0); // bord droit   -> chunk x+1
		if (position.y == 0)        updateVoisin(maps, 0, -1, 0); // bord du bas  -> chunk y-1
		if (position.y == SIZE - 1) updateVoisin(maps, 0, +1, 0); // bord du haut -> chunk y+1
		if (position.z == 0)        updateVoisin(maps, 0, 0, -1); // bord nord    -> chunk z-1
		if (position.z == SIZE - 1) updateVoisin(maps, 0, 0, +1); // bord sud     -> chunk z+1
	}

	/** Dit au chunk decale de (dx, dy, dz) de refaire son mesh... s'il existe ! */
	private void updateVoisin(Maps maps, int dx, int dy, int dz) {
		Vector3i pos = new Vector3i(this.chunkPosition.x + dx, this.chunkPosition.y + dy, this.chunkPosition.z + dz);
		Chunk voisin = maps.worldMaps.get(pos); // null si ce chunk n'existe pas
		if (voisin != null) {
			voisin.youWantMeToUpdate();
		}
	}
	
	public Mesh buildMesh(Maps maps) {
		if(this.meshIsUpdate) {
			return chunkMesh;
		} else {
			if(chunkMesh!=null) {
				chunkMesh.cleanup();
			}
			//code du mesh
			List<Float> positions = new ArrayList<>();
			List<Float> uvs = new ArrayList<>();
			List<Integer> indices = new ArrayList<>();
			for (int x = 0; x < SIZE; x++)
				for (int y = 0; y < SIZE; y++)
					for (int z = 0; z < SIZE; z++) {
						if(blocks[x][y][z] != null && blocks[x][y][z] != BlockType.STAIRS) {
							BlockType btActuelle = blocks[x][y][z];
							if (!maps.isBlock(new Vector3f((chunkPosition.x*SIZE)+x, (chunkPosition.y*SIZE)+y + 1, (chunkPosition.z*SIZE)+z))) ajouterFace(positions, uvs, indices, FACE_DESSUS, x, y, z, btActuelle);
							if (!maps.isBlock(new Vector3f((chunkPosition.x*SIZE)+x, (chunkPosition.y*SIZE)+y - 1, (chunkPosition.z*SIZE)+z))) ajouterFace(positions, uvs, indices, FACE_DESSOUS, x, y, z, btActuelle);
							if (!maps.isBlock(new Vector3f((chunkPosition.x*SIZE)+x, (chunkPosition.y*SIZE)+y, (chunkPosition.z*SIZE)+z - 1))) ajouterFace(positions, uvs, indices, FACE_NORD, x, y, z, btActuelle);
							if (!maps.isBlock(new Vector3f((chunkPosition.x*SIZE)+x, (chunkPosition.y*SIZE)+y, (chunkPosition.z*SIZE)+z + 1))) ajouterFace(positions, uvs, indices, FACE_SUD, x, y, z, btActuelle);
							if (!maps.isBlock(new Vector3f((chunkPosition.x*SIZE)+x + 1, (chunkPosition.y*SIZE)+y, (chunkPosition.z*SIZE)+z))) ajouterFace(positions, uvs, indices, FACE_EST, x, y, z, btActuelle);
							if (!maps.isBlock(new Vector3f((chunkPosition.x*SIZE)+x - 1, (chunkPosition.y*SIZE)+y, (chunkPosition.z*SIZE)+z))) ajouterFace(positions, uvs, indices, FACE_OUEST, x, y, z, btActuelle);
						}
					}
			this.meshIsUpdate=true;
			this.chunkMesh = creerMesh(positions, uvs, indices);
			return this.chunkMesh;
		}
	}
	
	static void ajouterFace(List<Float> positions, List<Float> uvs, List<Integer> indices,
			float[][] coins, float x, float y, float z, BlockType bt) {

		int base = positions.size() / 3; // nb de sommets deja dans la liste (3 floats par sommet)

		// les 4 coins, decales a la position du bloc
		for (float[] coin : coins) {
			positions.add(coin[0] + x);
			positions.add(coin[1] + y);
			positions.add(coin[2] + z);
		}

		// les UV : toujours l'image entiere sur la face
		float u0 = (float) (bt.getAtlasIndex()/4.0);
		float u1 = (float) (u0+0.25);
		uvs.add(u0); uvs.add(0f);
		uvs.add(u1); uvs.add(0f);
		uvs.add(u1); uvs.add(1f);
		uvs.add(u0); uvs.add(1f);

		// les 2 triangles, decales de "base"
		indices.add(base + 0); indices.add(base + 1); indices.add(base + 2);
		indices.add(base + 2); indices.add(base + 3); indices.add(base + 0);
	}
	
	static Mesh creerMesh(List<Float> positions, List<Float> uvs, List<Integer> indices) {
		float[] p = new float[positions.size()];
		for (int i = 0; i < p.length; i++) p[i] = positions.get(i);
		float[] u = new float[uvs.size()];
		for (int i = 0; i < u.length; i++) u[i] = uvs.get(i);
		int[] idx = new int[indices.size()];
		for (int i = 0; i < idx.length; i++) idx[i] = indices.get(i);
		return new Mesh(p, u, idx);
	}

}
