package ai.aliz.gcpmeetup.objectify;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.insight.BucketFactory;
import com.googlecode.objectify.insight.Codepointer;
import com.googlecode.objectify.insight.Collector;
import com.googlecode.objectify.insight.InsightAsyncDatastoreService;
import com.googlecode.objectify.insight.Recorder;

public class MyObjectifyFilter extends ObjectifyFilter {
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		Recorder recorder = new Recorder(new BucketFactory(), new Collector(), new Codepointer());
		recorder.setRecordAll(true);
		ObjectifyService.setFactory(new ObjectifyFactory() {
			@Override
			protected AsyncDatastoreService createRawAsyncDatastoreService(DatastoreServiceConfig cfg) {
				AsyncDatastoreService service = super.createRawAsyncDatastoreService(cfg);
				return new InsightAsyncDatastoreService(service, recorder);
			}
		});
	}

}
