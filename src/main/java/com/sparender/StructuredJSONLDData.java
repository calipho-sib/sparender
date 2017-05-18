package com.sparender;

/**
 * https://developers.google.com/search/docs/guides/intro-structured-data
 * @author Daniel Teixeira http://github.com/ddtxra
 */
public class StructuredJSONLDData {

	private String context = "http://schema.org/";
	private String type = "WebSite";
	private String url;
	private String name;
	
	public StructuredJSONLDData(String type, String url, String name){
		this.type = type;
		this.url = url;
		this.name = name;
	}
	
	@Override
	public String toString(){

		StringBuffer sb = new StringBuffer();
		sb.append("<script type=\"application/ld+json\">");
		sb.append("{");
		sb.append("\"@context\": \"" + context + "\"");
		sb.append("\"@type\": \"" + type + "\"");
		sb.append("\"url\": \"" + url + "\"");
		sb.append("\"name\": \"" + name + "\"");
		sb.append("}");
		sb.append("</script>");
		
		return sb.toString();
		
	}

	public void setContext(String context) {
		this.context = context;
	}
	public void setType(String type) {
		this.type = type;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setName(String name) {
		this.name = name;
	}
}
