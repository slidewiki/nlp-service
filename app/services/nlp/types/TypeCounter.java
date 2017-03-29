package services.nlp.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import services.util.MapCounting;

public class TypeCounter {
	
	public static Map<String,Integer> getTypeCountings(String[] tokens, boolean toLowerCase){
		Map<String,Integer> countings = new HashMap<>();
		for (String token : tokens) {
			String tokenToUse = token;
			if(toLowerCase){
				tokenToUse = token.toLowerCase();
			}
			MapCounting.addToCountingMap(countings, tokenToUse);
		}
		return countings;
	}

	public static Map<String,Integer> getTypeCountings(Collection<String> tokens, boolean toLowerCase){
		Map<String,Integer> countings = new HashMap<>();
		for (String token : tokens) {
			String tokenToUse = token;
			if(toLowerCase){
				tokenToUse = token.toLowerCase();
			}
			MapCounting.addToCountingMap(countings, tokenToUse);
		}
		return countings;
	}
}
