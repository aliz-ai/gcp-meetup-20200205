package ai.aliz.gcpmeetup;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.googlecode.objectify.ObjectifyService;

import ai.aliz.gcpmeetup.entity.GameState;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

@Log
@WebServlet("/tasks/worker")
public class AnalyzerServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	@Override
	@SneakyThrows
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String id = req.getParameter("id");
		log("Worker working on: " + id);
		// simulate a pretty heavy computation we can do on 3^9 states
		Thread.sleep(20_000);
		GameState gameState = ObjectifyService.ofy().load().type(GameState.class).id(id).now();
		BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
		TableId tableId = TableId.of("game_stats", "outcomes");
		Map<String, Object> rowContent = new HashMap<>();
		rowContent.put("game_id", id);
		rowContent.put("final_field", gameState.getFields());
		rowContent.put("outcome", gameState.getResult().name());
		rowContent.put("timestamp", Instant.now().getEpochSecond());
		InsertAllResponse response = bigquery.insertAll(InsertAllRequest.newBuilder(tableId).addRow(rowContent).build());
		if (response.hasErrors()) {
			log.severe("BQ insert errors: " + response.getInsertErrors());
		}
	}

}
