package shadows.stonerecipes.util;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;

/**
 * Data class to represent a note block.
 */
public class InstrumentalNote {

	private final Instrument instrument;
	private final Note note;

	public InstrumentalNote(Instrument instr, int note) {
		this(instr, new Note(note));
	}

	public InstrumentalNote(Instrument instr, Note note) {
		this.instrument = instr;
		this.note = note;
	}

	public boolean match(Block other) {
		NoteBlock noteBlock = (NoteBlock) other.getState().getBlockData();
		return match(noteBlock);
	}

	public boolean match(NoteBlock other) {
		return this.instrument.equals(other.getInstrument()) && this.note.equals(other.getNote());
	}

	public BlockData asBlockData() {
		BlockData data = Bukkit.createBlockData(Material.NOTE_BLOCK);
		((NoteBlock) data).setInstrument(instrument);
		((NoteBlock) data).setNote(note);
		return data;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public Note getNote() {
		return note;
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj.getClass() == InstrumentalNote.class && ((InstrumentalNote) obj).instrument == instrument && ((InstrumentalNote) obj).note.equals(note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(instrument, note);
	}

}
