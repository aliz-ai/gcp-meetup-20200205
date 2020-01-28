package ai.aliz.gcpmeetup;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.HttpRequest;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import ai.aliz.gcpmeetup.entity.ActiveGame;
import ai.aliz.gcpmeetup.entity.GameState;
import ai.aliz.gcpmeetup.entity.GameState.GameResult;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

@Log
public class TicTacService {
	
	private static final int [] [] sequences = {
			{0, 1, 2}, {3, 4, 5}, {6, 7, 8},
			{0, 3, 6}, {1, 4, 7}, {2, 5, 8},
			{0, 5, 8}, {2, 4, 6}
	};

	public GameState getOrCreateActiveGame(String sessionId) {
		return getOrCreateAndUpdateActiveGame(sessionId, gameState -> false);
	}
	
	protected GameState getOrCreateAndUpdateActiveGame(String sessionId, Function<GameState, Boolean> work) {
		ActiveGame activeGame = ofy().load().type(ActiveGame.class).id(sessionId).now();
		if (activeGame != null) {
			GameState gameState = ofy().load().type(GameState.class).id(activeGame.getGameId()).now();
			if (gameState != null && gameState.isActive()) {
				if (Strings.isNullOrEmpty(gameState.getSessionIdB())) {
					GameState existingGame = tryJoinAnOpenGame(sessionId);
					if (existingGame != null) {
						gameState.setActive(false);
						gameState.setResult(GameResult.Abandoned);
						ofy().save().entities(gameState);
						// don't apply work here
						return existingGame;
					}
				}
				if (work.apply(gameState)) {
					ofy().save().entities(gameState);
				}
				return gameState;
			}
		}
		GameState gameState = null;
		gameState = tryJoinAnOpenGame(sessionId);
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
		work.apply(gameState);
		ofy().save().entities(activeGame, gameState);
		return gameState;
	}

	private GameState tryJoinAnOpenGame(String sessionId) {
		// try to assign an existing game, retrying on lock conflict 5 times
		for (int retries = 0; retries < 5; retries ++) {
			List<GameState> partnerGames = ofy().load().type(GameState.class).filter("sessionIdB", "")
						.filter("active", true)
						.limit(10).list();
			partnerGames.removeIf(gameState -> gameState.getSessionIdA().equals(sessionId)); // don't choose our own game
			if (partnerGames.size() > 0) {
				// try lock a game
				GameState gameState = ofy().transact(() -> {
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
				if (gameState != null) {
					return gameState;
				}
			} else {
				break; // if there are no available games, we shouldn't rerty
			}
		}
		return null;
	}

	public GameState place(String sessionId, int index) {
		Preconditions.checkArgument(0 <= index && index < 9, index + " should be in range 0..8");
		return getOrCreateAndUpdateActiveGame(sessionId, gameState -> {
			boolean isUserA = gameState.getSessionIdA().equals(sessionId);
			Preconditions.checkState(gameState.getRound() % 2 == (isUserA ? 0 : 1)); // ensure proper round
			String stateString = gameState.getFields();
			Preconditions.checkState(stateString.charAt(index) == ' ');
			gameState.setFields(stateString.substring(0, index) + (isUserA ? 'o' : 'x') + stateString.substring(index + 1));
			gameState.setRound(gameState.getRound() + 1);
			GameResult result = checkAndApplyGameEnd(gameState.getFields());
			if (result != null) {
				gameState.setResult(result);
				reportGameOutcome(gameState.getId());
			}
			return true;
		});
	}
	
	protected GameResult checkAndApplyGameEnd(String fields) {
		for (int [] sequence : sequences) {
			String line = "" + fields.charAt(sequence[0]) + fields.charAt(sequence[1]) + fields.charAt(sequence[2]);
			if ("ooo".equals(line)) {
				return GameResult.ClientAWon;
			}
			if ("xxx".equals(line)) {
				return GameResult.ClientBWon;
			}
		}
		if (fields.indexOf(' ') == -1) {
			// no winner but no fields left
			return GameResult.Draw;
		}
		return null;
	}
	
	public GameState abandonCurrent(String sessionId) {
		getOrCreateAndUpdateActiveGame(sessionId, gameState -> {
			gameState.setActive(false);
			if (gameState.getResult() == null) {
				gameState.setResult(GameResult.Abandoned);
			}
			reportGameOutcome(gameState.getId());
			return true;
		});
		return getOrCreateActiveGame(sessionId);
	}
	
	@SneakyThrows
	protected void reportGameOutcome(String gameId) {
		try {
			try (CloudTasksClient cloudTasksClient = CloudTasksClient.create()) {
				String queuePath = QueueName.of("gcp-meetup-20200205", "us-central1", "analysis").toString();
				Task.Builder builder = Task.newBuilder()
							.setHttpRequest(HttpRequest.newBuilder()
										.setUrl("https://gcp-meetup-20200205.appspot.com/tasks/worker?id=" + gameId)
										.setHttpMethod(HttpMethod.GET).build());
				cloudTasksClient.createTask(queuePath, builder.build());
				log.info("Started analyzer task for: " + gameId);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Couldn't start analyzer for game: " + gameId, e);
		}
	}
}
