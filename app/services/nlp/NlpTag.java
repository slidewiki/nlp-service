package services.nlp;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.Span;

public class NlpTag {
	
	private String name;
	private String type;
	private String sourceName;
	private double probability;
	private String link;

	
	public NlpTag(String name, String type, String sourceName, double probability, String link) {
		super();
		this.name = name;
		this.type = type;
		this.sourceName = sourceName;
		this.probability = probability;
		this.link = link;
	}



	public NlpTag(Span span, String[] tokens, String sourceName){
		this.name = NER_OpenNLP.getTokenStringFromSpan(span, tokens);
		this.type = span.getType();
		this.probability = span.getProb();
		this.sourceName = sourceName;
	}
	
	public static List<NlpTag> fromSpans(Span[] spans, String[] tokens, String sourceName){
		List<NlpTag> result = new ArrayList<>();
		for (Span span : spans) {
			result.add(new NlpTag(span, tokens, sourceName));
		}
		return result;
	}



	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}



	public String getType() {
		return type;
	}



	public void setType(String type) {
		this.type = type;
	}



	public String getSourceName() {
		return sourceName;
	}



	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}



	public double getProbability() {
		return probability;
	}



	public void setProbability(double probability) {
		this.probability = probability;
	}
	
	
	public String getLink() {
		return link;
	}



	public void setLink(String link) {
		this.link = link;
	}


}
