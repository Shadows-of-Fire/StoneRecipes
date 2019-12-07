package shadows.stonerecipes.util;

import java.util.ArrayList;
import java.util.List;

import shadows.stonerecipes.StoneRecipes;

public class TickHandler {

	private static final List<ITickable> TICKERS = new ArrayList<>();

	public static void tick() {
		StoneRecipes.debug("Preparing to tick %s note block tiles...", TICKERS.size());
		for (int i = 0; i < TICKERS.size(); i++) {
			ITickable tick = TICKERS.get(i);
			if (tick.isDead()) TICKERS.remove(i--);
			else tick.tick();
		}
	}

	public static interface ITickable {
		void tick();

		boolean isDead();
	}

	public static void registerTickable(ITickable obj) {
		StoneRecipes.debug("%s has registered to tick.", obj);
		TICKERS.add(obj);
	}

}
