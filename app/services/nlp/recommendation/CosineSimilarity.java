package services.nlp.recommendation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CosineSimilarity {


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
			
//			double magnitude1 = calculateMagnitudeOfVector(tfidfMap1.values());
//	        double magnitude2 = calculateMagnitudeOfVector(tfidfMap2.values());
	        
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
