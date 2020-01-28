package ai.aliz.gcpmeetup;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.googlecode.objectify.ObjectifyService;

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
		final GameState game;
		if (placeParam != null) {
			int index = Integer.parseInt(placeParam);
			game = new TicTacService().place(session.getId(), index);
		} else {
			game = new TicTacService().getOrCreateActiveGame(session.getId());
		}
		resp.getWriter().print(game.getGameState());
	}

}
