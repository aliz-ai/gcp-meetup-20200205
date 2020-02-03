package ai.aliz.gcpmeetup;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.googlecode.objectify.ObjectifyService;

import ai.aliz.gcpmeetup.TicTacService.SessionAndGame;
import ai.aliz.gcpmeetup.entity.ActiveGame;
import ai.aliz.gcpmeetup.entity.GameState;

// sorry, no proper REST
@WebServlet("/tictac")
public class TicTacServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public void init() throws ServletException {
		super.init();
		ObjectifyService.register(ActiveGame.class);
		ObjectifyService.register(GameState.class);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpSession session = req.getSession(true);
		String placeParam = req.getParameter("place");
		String sessionId = session.getId();
		TicTacService ticTacService = new TicTacService();
		final SessionAndGame game;
		if (!Strings.isNullOrEmpty(req.getParameter("initial"))) {
			ticTacService.touchSession(sessionId);
		}
		if (!Strings.isNullOrEmpty(req.getParameter("abandon"))) {
			log("Abandoning game", new RuntimeException("Game abandoned"));
			game = ticTacService.abandonCurrent(sessionId);
		} else {
			if (placeParam != null) {
				int index = Integer.parseInt(placeParam);
				game = ticTacService.place(sessionId, index);
			} else {
				game = ticTacService.getOrCreateActiveGame(sessionId);
			}
		}
		GameInfo gameInfo = GameInfo.from(sessionId, game);
		String infoJson = new Gson().toJson(gameInfo);
		resp.getWriter().print(infoJson);
	}

}
