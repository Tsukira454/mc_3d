package mc3d;

public enum BlockType {
	DIRT(ModelType.cube, "textures/block/dirt.png", BlockSpawn.NATURAL),
	STAIRS(ModelType.stairs, "textures/block/stairs_default.png", BlockSpawn.NonNATURAL),
	DEFAULT(ModelType.cube, "textures/block/default.png", BlockSpawn.NonNATURAL),
	GOLD_BLOCK(ModelType.cube, "textures/block/gold_block.png", BlockSpawn.NonNATURAL),
	STONE(ModelType.cube, "textures/block/stone.png", BlockSpawn.NATURAL);

	// les champs qui stockent les valeurs de chaque constante
	private final ModelType model;
	private final String texturePath;
	private final BlockSpawn blockSpawn;

	// le constructeur (implicitement privé) : appelé 1× par constante
	BlockType(ModelType cube, String texturePath, BlockSpawn natural) {
		this.model = cube;
		this.texturePath = texturePath;
		this.blockSpawn = natural;
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
}