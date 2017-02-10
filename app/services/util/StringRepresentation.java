package services.util;

import java.util.List;

/**
 * Convenience class for getting a String representation of certain objects.
 * @author aschlaf
 *
 */
public class StringRepresentation {
	
	/**
	 * Returns a String of the list entries separated by the given separator.
	 * @param input The list
	 * @param separator The separator
	 * @return a String of the list entries separated by the given separator.
	 */
	public static <E> String fromList(List<E> input, String separator){
		
		if(input.size()==0){
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.size(); i++) {
			if(i>0){
				sb.append(separator);
			}
			E currentEntry = input.get(i);
			if(currentEntry!= null){
				sb.append(input.get(i).toString());

			}else{
				sb.append("NULL");
			}
		}
		return sb.toString();
	}

	/**
	 * Returns a String of the array entries separated by the given separator.
	 * @param input The array
	 * @param separator The separator
	 * @return a String of the array entries separated by the given separator.
	 */
	public static <E> String fromArray(E[] input, String separator){
		
		if(input.length==0){
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length; i++) {
			if(i>0){
				sb.append(separator);
			}
			E currentEntry = input[i];
			if(currentEntry!= null){
				sb.append(input[i].toString());

			}else{
				sb.append("NULL");
			}
		}
		return sb.toString();
	}
}
