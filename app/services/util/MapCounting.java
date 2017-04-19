package services.util;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
	
	public static <E> void addToCountingMap(Map<E,Double> map, E input, Double valueToAdd){
		
		if(map.containsKey(input)){
			Double valueInMap = map.get(input);
			Double valueToSet = new Double(valueInMap+valueToAdd);
			map.put(input, valueToSet);
		}else{
			map.put(input, new Double(valueToAdd));
		}
	}
}
