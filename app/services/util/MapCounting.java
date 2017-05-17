package services.util;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapCounting {

	public static <E> void addToCountingMap(Map<E,Integer> map, E input){
		
		if(map.containsKey(input)){
			Integer valueInMap = map.get(input);
			Integer valueToSet = new Integer(valueInMap+1);
			map.put(input, valueToSet);
		}else{
			map.put(input, new Integer(1));
		}
	}
	
	public static <E> Map<E,Integer> retrieveCountingMap(Collection<E> input){
		
		Map<E,Integer> map = new HashMap<>();
		for (E inputEntry : input) {
			addToCountingMap(map, inputEntry);
		}
		
		return map;
	}
	
	public static <E> void addToCountingMapAddingDoubleValue(Map<E,Double> map, E input, Double valueToAdd){
		
		if(map.containsKey(input)){
			Double valueInMap = map.get(input);
			Double valueToSet = new Double(valueInMap+valueToAdd);
			map.put(input, valueToSet);
		}else{
			map.put(input, new Double(valueToAdd));
		}
	}
	
	public static <E> void addToCountingMapAddingIntegerValue(Map<E,Integer> map, E input, Integer valueToAdd){
		
		if(map.containsKey(input)){
			Integer valueInMap = map.get(input);
			Integer valueToSet = new Integer(valueInMap+valueToAdd);
			map.put(input, valueToSet);
		}else{
			map.put(input, new Integer(valueToAdd));
		}
	}
	
	public static <E> void addToCountingMapAddingIntegerValues(Map<E,Integer> map, Map<E,Integer> mapToAddToGivenMap){
		
		Set<E> keysToAdd = mapToAddToGivenMap.keySet();
		for (E key : keysToAdd) {
			addToCountingMapAddingIntegerValue(map, key, mapToAddToGivenMap.get(key));
		}
		
	}
	
	public static <E> void addToCountingMapAddingIntegerValuesWithFactor(Map<E,Integer> map, Map<E,Integer> mapToAddToGivenMap, Integer factorToMultipleValuesBeforeAdding){
		
		Set<E> keysToAdd = mapToAddToGivenMap.keySet();
		for (E key : keysToAdd) {
			Integer value = mapToAddToGivenMap.get(key);
			Integer valueToUse = value * factorToMultipleValuesBeforeAdding;
			addToCountingMapAddingIntegerValue(map, key, valueToUse);
		}
		
	}
}
