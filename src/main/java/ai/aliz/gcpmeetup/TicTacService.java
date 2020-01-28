package ai.aliz.gcpmeetup;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import ai.aliz.gcpmeetup.entity.ActiveGame;
import ai.aliz.gcpmeetup.entity.GameState;

public class TicTacService {

	public GameState getOrCreateActiveGame(String sessionId) {
		return getOrCreateAndUpdateActiveGame(sessionId, gameState -> {});
	}
	
	
	protected GameState getOrCreateAndUpdateActiveGame(String sessionId, Consumer<GameState> work) {
		ActiveGame activeGame = ofy().load().type(ActiveGame.class).id(sessionId).now();
		if (activeGame != null) {
			GameState gameState = ofy().load().type(GameState.class).id(activeGame.getGameId()).now();
			if (gameState != null && gameState.isActive()) {
				work.accept(gameState);
				ofy().save().entities(gameState);
				return gameState;
			}
		}
		GameState gameState = null;
		// try to assign an existsing game, retrying on lock conflict 5 times
		for (int retries = 0; retries < 5 && gameState == null; retries ++) {
			List<GameState> partnerGames = ofy().load().type(GameState.class).filter("sessionIdB", "").limit(10).list();
			if (partnerGames.size() > 0) {
				// try lock a game
				gameState = ofy().transact(() -> {
					GameState partnerGame = partnerGames.get((int) Math.floor(Math.random() * partnerGames.size()));
					if (Strings.isNullOrEmpty(partnerGame.getSessionIdB())) {
						partnerGame.setSessionIdB(sessionId);
						ActiveGame newGame = new ActiveGame();
						newGame.setSessionId(sessionId);
						newGame.setGameId(partnerGame.getId());
						ofy().save().entities(partnerGame, newGame);
						// TODO apply work?
						return partnerGame;
					}
					return null;
				});
			} else {
				break; // if there are no available games, we shouldn't rerty
			}
		}
		if (gameState != null) {
			return gameState;
		}
		// no existing found, create new
		gameState = new GameState();
		gameState.setSessionIdA(sessionId);
		gameState.setId(sessionId + "" + System.currentTimeMillis());
		activeGame = new ActiveGame();
		activeGame.setSessionId(sessionId);
		activeGame.setGameId(gameState.getId());
		work.accept(gameState);
		ofy().save().entities(activeGame, gameState);
		return gameState;
	}

	public GameState place(String sessionId, int index) {
		Preconditions.checkArgument(0 <= index && index < 9, index + " should be in range 0..8");
		return getOrCreateAndUpdateActiveGame(sessionId, gameState -> {
			boolean isUserA = gameState.getSessionIdA().equals(sessionId);
			Preconditions.checkState(gameState.getRound() % 2 == (isUserA ? 0 : 1)); // ensure proper round
			String stateString = gameState.getGameState();
			Preconditions.checkState(stateString.charAt(index) == ' ');
			gameState.setGameState(stateString.substring(0, index) + (isUserA ? 'o' : 'x') + stateString.substring(index + 1));
			gameState.setRound(gameState.getRound() + 1);
			// TODO check winning
			// TODO check game end
		});
	}
}
