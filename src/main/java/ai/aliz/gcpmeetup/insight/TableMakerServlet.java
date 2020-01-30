package ai.aliz.gcpmeetup.insight;

import javax.servlet.annotation.WebServlet;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryRequestInitializer;
import com.google.api.services.bigquery.BigqueryScopes;
import com.googlecode.objectify.insight.puller.InsightDataset;
import com.googlecode.objectify.insight.servlet.AbstractTableMakerServlet;

import lombok.SneakyThrows;

@WebServlet("private/tableMaker")
public class TableMakerServlet extends AbstractTableMakerServlet {

	private static final long serialVersionUID = 1L;

	static final InsightDataset insightDataset = new InsightDataset() {
		@Override
		public String projectId() {
			return "gcp-meetup-20200205";
		}

		@Override
		public String datasetId() {
			return "insight";
		}
	};

	@Override
	protected InsightDataset insightDataset() {
		return insightDataset;
	}

	@Override
	protected Bigquery bigquery() {
		return buildBigquery();
	}

	@SneakyThrows
	protected static Bigquery buildBigquery() {
		JsonFactory jsonFactory = new JacksonFactory(); 
		GoogleCredential credential = GoogleCredential.getApplicationDefault(new UrlFetchTransport(), jsonFactory);
		credential = credential.createScoped(BigqueryScopes.all());
		return new Bigquery.Builder(new UrlFetchTransport(), Utils.getDefaultJsonFactory(), credential)
				.setApplicationName("gcp-meetup-20200205")
				.setGoogleClientRequestInitializer(new BigqueryRequestInitializer()).build();
	}

}
