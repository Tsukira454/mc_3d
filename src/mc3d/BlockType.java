package mc3d;

public enum BlockType {
	DIRT(ModelType.cube, BlockSpawn.NATURAL, new int[] { 1, 1, 1, 1, 1, 1 }),
	STAIRS(ModelType.stairs, BlockSpawn.NonNATURAL,
			new int[] { -1, -1, -1, -1, -1, -1 }),
	DEFAULT(ModelType.cube, BlockSpawn.NonNATURAL, new int[] { 0, 0, 0, 0, 0, 0 }),
	GOLD_BLOCK(ModelType.cube, BlockSpawn.NonNATURAL, new int[] { 3, 3, 3, 3, 3, 3 }),
	STONE(ModelType.cube, BlockSpawn.NATURAL, new int[] { 2, 2, 2, 2, 2, 2 }),
	GRASS_BLOCK(ModelType.cube, BlockSpawn.NATURAL, new int[] { 5, 1, 4, 4, 4, 4 });

	// les champs qui stockent les valeurs de chaque constante
	private final ModelType model;
	private final BlockSpawn blockSpawn;
	private int[] atlasFaces;
	// haut | bas | nords | sud | est | ouest

	// le constructeur (implicitement privé) : appelé 1× par constante
	BlockType(ModelType cube, BlockSpawn natural, int[] atlasIndex) {
		this.model = cube;
		this.blockSpawn = natural;
		this.atlasFaces = atlasIndex;
	}

	// les getters pour lire les valeurs
	public ModelType getModel() {
		return model;
	}

	public BlockSpawn getBlockSpawn() {
		return blockSpawn;
	}

	public int[] getAtlasIndex() {
		return atlasFaces;
	}

	public int getAtlasIndex(int face) {
		return atlasFaces[face];
	}
}