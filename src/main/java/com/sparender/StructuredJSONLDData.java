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
	private String description;
	
	public StructuredJSONLDData(String type, String url, String name, String description){
		this.type = type;
		this.url = url;
		this.name = name;
		this.description = description;
	}
	
	@Override
	public String toString(){

		StringBuffer sb = new StringBuffer();
		sb.append("<script type=\"application/ld+json\">\n");
		sb.append("{\n");
		sb.append("\t\"@context\": \"" + context + "\",\n");
		sb.append("\t\"@type\": \"" + type + "\",\n");
		sb.append("\t\"url\": \"" + url + "\",\n");
		sb.append("\t\"name\": \"" + name + "\",\n");
		sb.append("\t\"description\": \"" + description + "\"\n");
		sb.append("}\n");
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
	public void setDescription(String description) {
		this.description = description;
	}

}
