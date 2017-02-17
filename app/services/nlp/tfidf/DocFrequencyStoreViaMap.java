package services.nlp.tfidf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import services.util.Sorter;

public class DocFrequencyStoreViaMap implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6706095131200562001L;
	
	private Map<String,Integer> mapTermToDocFrequencies;
	private int numberOfDocs;
	
	
	
	public DocFrequencyStoreViaMap(Map<String, Integer> mapTermToDocFrequencies, int numberOfDocs) {
		
		this.mapTermToDocFrequencies = mapTermToDocFrequencies;
		this.numberOfDocs = numberOfDocs;
	}

	public DocFrequencyStoreViaMap(){
		
		this.numberOfDocs = 0;
		this.mapTermToDocFrequencies = new HashMap<>();
	}
	
	public void addDocument(Set<String> termtypesOfDoc){
		this.numberOfDocs++;
		for (String term : termtypesOfDoc) {
			if(this.mapTermToDocFrequencies.containsKey(term)){
				Integer valueInMap = mapTermToDocFrequencies.get(term);
				this.mapTermToDocFrequencies.put(term, new Integer(valueInMap+1));
			}else{
				this.mapTermToDocFrequencies.put(term, new Integer(1));
			}
		}		
	}
	
	public void removeDocument(Set<String> termtypesOfDoc){
		this.numberOfDocs--;
		for (String term : termtypesOfDoc) {
			if(this.mapTermToDocFrequencies.containsKey(term)){
				Integer valueInMap = mapTermToDocFrequencies.get(term);
				if(valueInMap.equals(new Integer(1))){
					mapTermToDocFrequencies.remove(term);
				}else{
					this.mapTermToDocFrequencies.put(term, new Integer(valueInMap-1));
				}
			}else{
				this.mapTermToDocFrequencies.put(term, new Integer(1));
			}
		}		
	}
	
	
	public Integer getDocFrequency(String term) {
		if(this.mapTermToDocFrequencies.containsKey(term)){
			return mapTermToDocFrequencies.get(term);
		}else{
			return 0;
		}
		
	}

	
	public Integer getNumberOfAllDocs() {
		return this.numberOfDocs;
	}
	
	public static DocFrequencyStoreViaMap deserializeFromFile(String filepath) throws FileNotFoundException, IOException, ClassNotFoundException{
		
		FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
                fis = new FileInputStream(filepath);
                in = new ObjectInputStream(fis);
                DocFrequencyStoreViaMap result = (DocFrequencyStoreViaMap) in.readObject();
                return result;
        } 
        finally {
            IOUtils.closeQuietly(in);
        }
	}

	public static void serialize(DocFrequencyStoreViaMap docFrequencyProvider, String filepath) throws FileNotFoundException, IOException, ClassNotFoundException{

		FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
                fos = new FileOutputStream(filepath);
                out = new ObjectOutputStream(fos);
                out.writeObject(docFrequencyProvider);
                
        } finally {
            IOUtils.closeQuietly(out);
        }
	}
	
	public List<Entry<String,Integer>> getTopXTypesBasedOnDocFreq(int x){
		List<Entry<String,Integer>> sorted = Sorter.sortByValueAndReturnAsList(this.mapTermToDocFrequencies, true);
		if(this.mapTermToDocFrequencies.size()>x){
			return sorted.subList(0, x);		
		}else{
			return sorted;
		}
		 
	}
	
	public String getTopXTypesBasedOnDocFreqAsString(int x, String delimiterBetweenEntries, boolean outputNumDocsForType, String delimiterBetweenTypeAndNumDocs){
		List<Entry<String,Integer>> entries = getTopXTypesBasedOnDocFreq(x);
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Integer> entry : entries) {
			sb.append(entry.getKey());
			if(outputNumDocsForType){
				sb.append(delimiterBetweenTypeAndNumDocs);
				sb.append(entry.getValue());
			}
			sb.append(delimiterBetweenEntries);
		}
		return sb.toString();
	}
}
