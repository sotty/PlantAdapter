package quantities;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import unit.DigitalUnit;

// TODO separare quantity da DigitalAmount per separare concetto informatico da quello elettronico (implementare classe DigitalQuantifiedAmount, livelli correlati ad una grandezza fisica per ottenere EndpointInterface adatta)
public class DigitalAmount implements IDigitalAmount {
	/**
	 * Livello della quantit� digitale
	 */
	private int level;
	private DigitalQuantity levelQuantity;
	
	public DigitalAmount(int level, DigitalQuantity levelQuantity) {
		if(level < 0 || level >= levelQuantity.getLevels())
			throw new IllegalArgumentException("DigitalLevel not in range");
		this.level = level;
		this.levelQuantity = levelQuantity;
	}
	
	public int getLevel() {
		return level;
	}
	
	@Override
	public Class<? extends Quantity> getQuantity() {
		return levelQuantity.getClass();
	}

	@Override
	public int getLevels() {
		return levelQuantity.getLevels();
	}

	@Override
	public Unit<? extends Quantity> getUnit() {
		return DigitalUnit.getDigitalUnit(levelQuantity.getLevels());
	}
	
	@Override
	public String toString()
	{
		return this.getLevel() + "x" + this.getLevels();
	}

}