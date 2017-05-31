package services.nlp.ner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import opennlp.tools.util.Span;
import services.util.MapCounting;

public class NerAnnotation {
	
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
	public NerAnnotation(@JsonProperty("name") String name, @JsonProperty("type") String type, @JsonProperty("sourceName") String sourceName, @JsonProperty("probability") double probability, @JsonProperty("tokenSpanBegin") int tokenSpanBegin, @JsonProperty("tokenSpanEnd") int tokenSpanEnd, @JsonProperty("link") String link) {
		super();
		this.name = name;
		this.type = type;
		this.sourceName = sourceName;
		this.probability = probability;
		this.tokenSpanBegin = tokenSpanBegin;
		this.tokenSpanEnd = tokenSpanEnd;
		this.link = link;
	}


	

	public NerAnnotation(Span span, String[] tokens, String sourceName){
		this.name = NER_OpenNLP.getTokenStringFromSpan(span, tokens);
		this.type = span.getType();
		this.probability = span.getProb();
		this.sourceName = sourceName;
		this.tokenSpanBegin = span.getStart();
		this.tokenSpanEnd = span.getEnd();
		this.link = "";
	}
	
	public static List<NerAnnotation> fromSpans(Span[] spans, String[] tokens, String sourceName){
		List<NerAnnotation> result = new ArrayList<>();
		for (Span span : spans) {
			result.add(new NerAnnotation(span, tokens, sourceName));
		}
		return result;
	}

	public static List<NerAnnotation> filterForNERsWithGivenTokenSpans(List<NerAnnotation> listToFilter, int minTokenSpan, int maxTokenSpan){
		List<NerAnnotation> result = new ArrayList<>();
		
		for (NerAnnotation ne : listToFilter) {
			if(ne.getTokenSpanBegin() < minTokenSpan){
				continue;
			}
			if(ne.getTokenSpanEnd()> maxTokenSpan){
				continue;
			}
			result.add(ne);
		}
		
		return result;
	}

	/**
	 * Retrieves the Named Entity frequencies.
	 * If several NER methods were used, the same entity might be recognized more than once.
	 * To count these entities only once, the process includes identity check via spans. If the same span was already counted, it will be skipped.
	 * @param nes the list of named entities to analyze
	 * @param toLowerCase if true, the named entities are {@link Transformed} to lower case for counting
	 * @return
	 */
	public static Map<String,Integer> getNERFrequenciesByAnalyzingNEs(List<NerAnnotation> nes, boolean toLowerCase){
		
		Map<String,Integer> result = new HashMap<>(); 
		Set<String> tokenSpans= new HashSet<>(); // tracks tokenSpans (as String begin_end for counting NEs detected by several sources only once (identity is defined here by the token spans))

		for (NerAnnotation nerEntity : nes) {
			
			int tokenspanBegin = nerEntity.getTokenSpanBegin();
			if(tokenspanBegin>=0){// only do tracking if token spans available
				int tokenSpanEnd = nerEntity.getTokenSpanEnd();
				String tokenSpan = tokenspanBegin + "_" + tokenSpanEnd;
				if(tokenSpans.contains(tokenSpan)){
					continue;
				}
				tokenSpans.add(tokenSpan);
				String ne = nerEntity.getName();
				if(toLowerCase){
					ne = ne.toLowerCase();
				}
				MapCounting.addToCountingMap(result, ne);				
			}
			
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
