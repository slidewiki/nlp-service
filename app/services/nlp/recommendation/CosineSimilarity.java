package services.nlp.recommendation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

public class CosineSimilarity {

		public static ObjectNode getCosineSimilarityWithExtendedInfo(Map<String,Double> tfidfMap1, Map<String,Double> tfidfMap2, ObjectNode nodeToAddDetailsTo){
						
			// keep only those terms included in both
			Set<String> intersection = new HashSet<String>(tfidfMap1.keySet()); 
			intersection.retainAll(tfidfMap2.keySet());
			
			// calculate dot product
			double dotProduct = 0;
			ArrayNode arrayNodeSharedEntries = Json.newArray();
			
			for (String item : intersection) {
				double tfidfValue1 = tfidfMap1.get(item);
				double tfidfValue2 = tfidfMap2.get(item);
				
	            dotProduct += ( tfidfValue1 * tfidfValue2);
	            
	            ObjectNode node = Json.newObject();
	            node.put("entry", item);
	            node.put("tfidfValue1", tfidfValue1);
	            node.put("tfidfValue2", tfidfValue2);
	            arrayNodeSharedEntries.add(node);
	        }
			
			
			// calculate magnitude1
			Double sum1 = new Double(0);
			for (String item : tfidfMap1.keySet()) {
				Double square = Math.pow(tfidfMap1.get(item), 2);
				sum1 += square;
			}
			Double magnitude1 = Math.sqrt(sum1);
			
		
			// calculate magnitude2
			Double sum2 = new Double(0);
			for (String item : tfidfMap2.keySet()) {
				Double square = Math.pow(tfidfMap2.get(item), 2);
				sum2 += square;
			}
			Double magnitude2 = Math.sqrt(sum2);
				        
			double cosineSimilarity = 0;
			double productOfMagnitudes = magnitude1 * magnitude2;
			if(productOfMagnitudes!=0){
				cosineSimilarity = dotProduct / (magnitude1 * magnitude2);	
			}
			nodeToAddDetailsTo.put("cosineSimilarity", cosineSimilarity);
			nodeToAddDetailsTo.set("sharedEntriesWithinTopXConsidered", arrayNodeSharedEntries);

			return nodeToAddDetailsTo;
		}

		public static double getCosineSimilarity(Map<String,Double> tfidfMap1, Map<String,Double> tfidfMap2){
		   
			// keep only those terms included in both
			Set<String> intersection = new HashSet<String>(tfidfMap1.keySet()); 
			intersection.retainAll(tfidfMap2.keySet());
			
			// calculate dot product
			double dotProduct = 0;			
			for (String item : intersection) {
	            dotProduct += (tfidfMap1.get(item) * tfidfMap2.get(item));
	        }
			
			// calculate magnitude1
			Double sum1 = new Double(0);
			for (String item : tfidfMap1.keySet()) {
				Double square = Math.pow(tfidfMap1.get(item), 2);
				sum1 += square;
			}
			Double magnitude1 = Math.sqrt(sum1);
			
		
			// calculate magnitude2
			Double sum2 = new Double(0);
			for (String item : tfidfMap2.keySet()) {
				Double square = Math.pow(tfidfMap2.get(item), 2);
				sum2 += square;
			}
			Double magnitude2 = Math.sqrt(sum2);
				        
			double cosineSimilarity = 0;
			double productOfMagnitudes = magnitude1 * magnitude2;
			if(productOfMagnitudes!=0){
				cosineSimilarity = dotProduct / (magnitude1 * magnitude2);	
			}
	         
	       
	        return cosineSimilarity;			
		}
		
		
		
		public static double calculateDotProductOfVectors(Map<String,Double> tfidfMap1, Map<String,Double> tfidfMap2){
			
			// keep only those terms included in both
			Set<String> intersection = new HashSet<String>(tfidfMap1.keySet()); 
			intersection.retainAll(tfidfMap2.keySet());
			
			double dotProduct = 0;
		
			for (String item : intersection) {
	            dotProduct += tfidfMap1.get(item) * tfidfMap2.get(item);
	        }
			
			return dotProduct;
			
		}
	
		public static double calculateMagnitudeOfVector(Collection<Double> vector){
			
			Double sum = new Double(0);
			for (Double entry : vector) {
				Double square = Math.pow(entry, 2);
				sum =+ square;
			}
			Double result = Math.sqrt(sum);
			
			return result;
		}
		

}
