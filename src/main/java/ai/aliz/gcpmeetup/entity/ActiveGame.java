package ai.aliz.gcpmeetup.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import lombok.Data;

@Data
@Entity
public class ActiveGame {

	/**
	 * We ignore a player's request if it seems to have just left the browser tab open.
	 */
	public static final Duration InactivationTimeout = Duration.ofMinutes(1);
	
	@Id
	private String sessionId;
	
	@Index
	private String gameId;
	
	/**
	 * @see #InactivationTimeout
	 */
	private Date lastActive = new Date();

	public boolean hasTimedout() {
		return lastActive != null && Duration.between(lastActive.toInstant(), Instant.now()).compareTo(ActiveGame.InactivationTimeout) > 0;
	}

}
