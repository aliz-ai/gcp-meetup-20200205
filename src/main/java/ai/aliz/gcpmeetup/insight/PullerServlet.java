package ai.aliz.gcpmeetup.insight;

import javax.servlet.annotation.WebServlet;

import com.google.api.services.bigquery.Bigquery;
import com.googlecode.objectify.insight.puller.InsightDataset;
import com.googlecode.objectify.insight.servlet.AbstractPullerServlet;

@WebServlet("/private/puller")
public class PullerServlet extends AbstractPullerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected InsightDataset insightDataset() {
		return TableMakerServlet.insightDataset;
	}

	@Override
	protected Bigquery bigquery() {
		return TableMakerServlet.buildBigquery();
	}
	
}
