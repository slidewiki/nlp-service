package services.util;


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
	
	
}
