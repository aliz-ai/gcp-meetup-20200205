package ai.aliz.gcpmeetup.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import lombok.Data;

@Data
@Entity
public class GameState {
	
	@Id
	private String id;
	
	@Index
	private String sessionIdA;
	
	@Index
	// TODO could null be queried on?
	private String sessionIdB = "";
	
	@Index
	private boolean active = true;
	
	private GameResult result;
	
	/**
	 * Starts with 0, where player A can place an 'o'.
	 * In odd rounds user B places an 'x'.
	 */
	private int round = 0;
	
	/**
	 * Textual representation, each cell is either a space, either 'o' (session A) or 'x' (session B).
	 * 3 characters sequence for each row, 3 rows, 9 characters total.
	 */
	private String fields = "         ";

	public enum GameResult {
		ClientAWon,
		ClientBWon,
		Draw,
		Abandoned
	}
}
