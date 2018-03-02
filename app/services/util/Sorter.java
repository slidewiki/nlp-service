package services.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Convenience class for sorting tasks.
 *
 */
public class Sorter {

	/**
	 * Sorts a given map by its values.
	 * @param map The map.
	 * @param reverse If true, the sorting will be in reverse order.
	 * @return The map sorted by its values
	 */
	public static <E, F> Map<E, F> sortByValue(Map<E, F> map, boolean reverse) {

		List<Entry<E, F>> list = sortByValueAndReturnAsList(map, reverse);
		Map<E, F> result = new LinkedHashMap<E, F>();
		for (Iterator<Entry<E, F>> it = list.iterator(); it.hasNext();) {
			Map.Entry<E, F> entry = it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	/**
	 * Sorts the given map by value and returns it as a list of entries.
	 * @param map The map
	 * @param reverse If true, the sorting will be in reverse order.
	 * @return a list of the map entries sorted by its values
	 */
	public static <E, F> List<Entry<E, F>> sortByValueAndReturnAsList(Map<E, F> map, boolean reverse){
		
		List<Entry<E, F>> list = new LinkedList<Entry<E, F>>(map.entrySet());
		Collections.sort(list, new Comparator<Entry<E, F>>() {
			@SuppressWarnings("unchecked")
            public int compare(Entry<E, F> o1, Entry<E, F> o2) {
				return ((Comparable<F>) ((Map.Entry<E, F>) (o1)).getValue()).compareTo(((Map.Entry<E, F>) (o2)).getValue());
			}
		});
		if (reverse){
			Collections.reverse(list);

		}

		return list;
	}
	
	public static <E,F> Map<E,F> keepOnlyTopXValues(Map<E,F> map, int maximumTopXValuesToKeep){
		
		Map<E,F> result = new HashMap<>();
		List<Entry<E, F>> sortedList = sortByValueAndReturnAsList(map, true);
		int counter = 0;
		for (Entry<E, F> entry : sortedList) {
			counter++;
			if(counter>maximumTopXValuesToKeep){
				break;
			}
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
		
	}

}
