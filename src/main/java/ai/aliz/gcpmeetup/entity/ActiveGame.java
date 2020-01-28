package ai.aliz.gcpmeetup.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import lombok.Data;

@Data
@Entity
public class ActiveGame {
	
	@Id
	private String sessionId;
	
	@Index
	private String gameId;

}
