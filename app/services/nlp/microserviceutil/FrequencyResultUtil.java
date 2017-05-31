package services.nlp.microserviceutil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import play.libs.Json;
import services.nlp.tfidf.TermFrequency;

public class FrequencyResultUtil {
	
	private static String propertyNameLanguage = "language";
	private static String propertyNameFrequencyOfMostFrequentWord = "frequencyOfMostFrequentWord";
	private static String propertyNameTotalDocs = "totalDocs";
	private static String propertyNameDocsForLanguage = "docsForLanguage";
	
	
	public static List<TermFrequency> getTermFrequenciesFromFrequencyResultNode(JsonNode node, String keyname){
		
		List<TermFrequency> result = new ArrayList<>();
		JsonNode frequencyNode = node.get(keyname);
		if(!frequencyNode.isArray()){
			return result;
		}
		ArrayNode arraynode = (ArrayNode) frequencyNode;
		Iterator<JsonNode> iterator= arraynode.iterator();
		while(iterator.hasNext()){
			JsonNode entry = iterator.next();
			TermFrequency termFrequency = getTermFrequencyFromTermNode(entry);
			result.add(termFrequency);
		}
		return result;
	}
	
	private static TermFrequency getTermFrequencyFromTermNode(JsonNode node){
		return Json.fromJson(node, TermFrequency.class);
	}
	
	public static String getLanguage(JsonNode node){
		return node.get(propertyNameLanguage).asText();
	}
	
	public static int getfrequencyOfMostFrequentWord(JsonNode node){
		return node.get(propertyNameFrequencyOfMostFrequentWord).asInt();
	}
	
	public static int getNumberOfDocsTotal(JsonNode node){
		return node.get(propertyNameTotalDocs).asInt();
	}
	
	public static int getNumberOfDocsForLanguage(JsonNode node){
		return node.get(propertyNameDocsForLanguage).asInt();
	}
	
	public static int getNumberOfSlides(JsonNode node){
		return node.get(NLPResultUtil.propertyNameNumberOfSlides).asInt();
	}
	
	public static int getNumberOfSlidesWithText(JsonNode node){
		if(node.has(NLPResultUtil.propertyNameNumberOfSlidesWithText)){
			return node.get(NLPResultUtil.propertyNameNumberOfSlidesWithText).asInt();
		}
		return 0;
	}
	
}
