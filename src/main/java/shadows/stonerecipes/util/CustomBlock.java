package shadows.stonerecipes.util;

import java.util.Objects;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;

public class CustomBlock {

	protected final Material block;
	protected final BlockData data;

	public CustomBlock(Material block, @Nullable BlockData data) {
		this.block = block;
		this.data = data;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CustomBlock && ((CustomBlock) obj).block == this.block && Objects.equals(this.data, ((CustomBlock) obj).data);
	}

	@Override
	public int hashCode() {
		return data == null ? block.hashCode() : Objects.hash(block, data);
	}

	public void place(Block block) {
		block.setType(this.block);
		if (data != null) block.setBlockData(data.clone());
	}

	public boolean match(NoteBlock note) {
		return block == Material.NOTE_BLOCK && note.equals(data);
	}

}
