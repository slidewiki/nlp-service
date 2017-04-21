package services.nlp;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import opennlp.tools.util.Span;
import services.nlp.ner.NER_OpenNLP;

public class NlpAnnotation {
	
	private String name;
	private String type;
	private String sourceName;
	private double probability;
	private int tokenSpanBegin;
	private int tokenSpanEnd;
	private String link;

	
	/**
	 * 
	 * @param name
	 * @param type
	 * @param sourceName
	 * @param probability
	 * @param spanBegin the start of the NER span (the token index, not the char index) 
	 * @param spanEnd the end of the NER span (the token index, not the char index), which is +1 more than the last element in the span.
	 * @param link
	 */
	@JsonCreator
	public NlpAnnotation(@JsonProperty("name") String name, @JsonProperty("type") String type, @JsonProperty("sourceName") String sourceName, @JsonProperty("probability") double probability, @JsonProperty("tokenSpanBegin") int tokenSpanBegin, @JsonProperty("tokenSpanEnd") int tokenSpanEnd, @JsonProperty("link") String link) {
		super();
		this.name = name;
		this.type = type;
		this.sourceName = sourceName;
		this.probability = probability;
		this.tokenSpanBegin = tokenSpanBegin;
		this.tokenSpanEnd = tokenSpanEnd;
		this.link = link;
	}


	

	public NlpAnnotation(Span span, String[] tokens, String sourceName){
		this.name = NER_OpenNLP.getTokenStringFromSpan(span, tokens);
		this.type = span.getType();
		this.probability = span.getProb();
		this.sourceName = sourceName;
		this.tokenSpanBegin = span.getStart();
		this.tokenSpanEnd = span.getEnd();
		this.link = "";
	}
	
	public static List<NlpAnnotation> fromSpans(Span[] spans, String[] tokens, String sourceName){
		List<NlpAnnotation> result = new ArrayList<>();
		for (Span span : spans) {
			result.add(new NlpAnnotation(span, tokens, sourceName));
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


	/**
	 * Returns the begin of the NER span (the token index, not the char index)
	 * @return
	 */
	public int getTokenSpanBegin() {
		return tokenSpanBegin;
	}


	/**
	 * Returns the end of the NER span (the token index, not the char index), which is +1 more than the last element in the span.
	 * @return
	 */
	public int getTokenSpanEnd() {
		return tokenSpanEnd;
	}

	

}
