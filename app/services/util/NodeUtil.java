package services.util;

import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

public class NodeUtil {

	/**
	 * create an array node for an entry list List<Entry<String, Integer>> which would be typically a list of frequency entries
	 * @param entryList
	 * @param nameForKey
	 * @param nameForValue
	 * @return
	 */
	public static ArrayNode createArrayNodeFromStringIntegerEntryList(List<Entry<String,Integer>> entryList, String nameForKey, String nameForValue){
		ArrayNode arrayNode = Json.newArray();
		for (Entry<String,Integer> entry : entryList) {
			ObjectNode node = Json.newObject();
			node.put(nameForKey, entry.getKey());
			node.put(nameForValue, entry.getValue());
			arrayNode.add(node);
		}
		return arrayNode;
	}
	
	/**
	 * create an array node for an entry list List<Entry<String, Double>> which would be typically a list of tfidf entries
	 * @param list
	 * @param nameToUseForKey
	 * @param nameToUseForValue
	 * @return
	 */
	public static ArrayNode createArrayNodeFromStringDoubleEntryList(List<Entry<String, Double>> list, String nameToUseForKey, String nameToUseForValue){
		ArrayNode arrayNode = Json.newArray();
		for (Entry<String, Double> entry : list) {		
			ObjectNode singleNode = Json.newObject();
			singleNode.put(nameToUseForKey, entry.getKey());
			singleNode.put(nameToUseForValue, entry.getValue());
			arrayNode.add(singleNode);
		}
		return arrayNode;
	}

}
