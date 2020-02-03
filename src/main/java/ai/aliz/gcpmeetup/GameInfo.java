package ai.aliz.gcpmeetup;

import com.google.common.base.Strings;

import ai.aliz.gcpmeetup.TicTacService.SessionAndGame;
import ai.aliz.gcpmeetup.entity.GameState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameInfo {
	
	private String fields;
	
	private ClientState state;
	
	private boolean isClientA;
	
	public static GameInfo from(String sessionId, SessionAndGame sessionAndGame) {
		final ClientState state;
		GameState gameState = sessionAndGame.getGameState();
		if (sessionAndGame.isTimedOut()) {
			return new GameInfo(null, ClientState.PlayerInactive, false);
		}
		boolean isClientA = gameState.getSessionIdA().equals(sessionId);
		if (isClientA && Strings.isNullOrEmpty(gameState.getSessionIdB())) {
			state = ClientState.WaitingForOpponent;
		} else {
			if (gameState.getResult() == null) {
				// still active
				state = (isClientA == (gameState.getRound() % 2 == 0)) ? ClientState.MyTurn : ClientState.OpponentTurn; 
			} else {
				// game finished
				switch (gameState.getResult()) {
				case Abandoned:
					state = ClientState.Abandoned;
					break;
				case ClientAWon:
					state = isClientA ? ClientState.Won : ClientState.Lost;
					break;
				case ClientBWon:
					state = isClientA ? ClientState.Lost : ClientState.Won;
					break;
				case Draw:
					state = ClientState.Draw;
					break;
				default:
					throw new IllegalStateException();
				}
			}
		}
		return new GameInfo(gameState.getFields(), state, isClientA);
	}
	
	public enum ClientState {
		WaitingForOpponent,
		MyTurn,
		OpponentTurn,
		Won,
		Lost,
		Draw,
		Abandoned,
		PlayerInactive
	}

}
