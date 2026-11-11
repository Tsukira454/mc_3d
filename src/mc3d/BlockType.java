package mc3d;

public enum BlockType {
	DIRT(ModelType.cube, "textures/block/dirt.png", BlockSpawn.NATURAL, 1),
	STAIRS(ModelType.stairs, "textures/block/stairs_default.png", BlockSpawn.NonNATURAL,-1),
	DEFAULT(ModelType.cube, "textures/block/default.png", BlockSpawn.NonNATURAL,0),
	GOLD_BLOCK(ModelType.cube, "textures/block/gold_block.png", BlockSpawn.NonNATURAL,3),
	STONE(ModelType.cube, "textures/block/stone.png", BlockSpawn.NATURAL,2);

	// les champs qui stockent les valeurs de chaque constante
	private final ModelType model;
	private final String texturePath;
	private final BlockSpawn blockSpawn;
	private int atlasIndex;

	// le constructeur (implicitement privé) : appelé 1× par constante
	BlockType(ModelType cube, String texturePath, BlockSpawn natural, int atlasIndex) {
		this.model = cube;
		this.texturePath = texturePath;
		this.blockSpawn = natural;
		this.atlasIndex = atlasIndex;
	}

	// les getters pour lire les valeurs
	public ModelType getModel() {
		return model;
	}

	public String getTexturePath() {
		return texturePath;
	}

	public BlockSpawn getBlockSpawn() {
		return blockSpawn;
	}
	
	public int getAtlasIndex() {
		return atlasIndex;
	}
}