package com.sparender;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

public class RunningRequests {

	private AtomicLong counter = new AtomicLong();
	private Vector<String> requests;

	public RunningRequests() {
		requests = new Vector<>();
	}

	public void addRequest(String request){
		synchronized (requests) {
			counter.incrementAndGet();
			requests.add(request);
		}
	}

	public void removeRequest(String request){
		synchronized (requests) {
			requests.remove(request);
		}
	}


	public String getRunningRequests(){

		synchronized (requests){

			StringBuffer sb = new StringBuffer();

			sb.append("Running requests count: " + requests.size());
			sb.append("\n");

			requests.forEach(sb::append);

			sb.append("\n");
			sb.append("\n");
			sb.append("Total requests count: " + counter.get());
			return sb.toString();

		}

	}

}