package services.nlp.tfidf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

public class DocumentFrequencyProviderViaMap implements IDocFrequencyProvider, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6706095131200562001L;
	
	private Map<String,Integer> mapTermToDocFrequencies;
	private int numberOfDocs;
	
	
	
	public DocumentFrequencyProviderViaMap(Map<String, Integer> mapTermToDocFrequencies, int numberOfDocs) {
		
		this.mapTermToDocFrequencies = mapTermToDocFrequencies;
		this.numberOfDocs = numberOfDocs;
	}

	public DocumentFrequencyProviderViaMap(){
		
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
	
	@Override
	public Integer getDocFrequency(String term) {
		if(this.mapTermToDocFrequencies.containsKey(term)){
			return mapTermToDocFrequencies.get(term);
		}else{
			return 0;
		}
		
	}

	@Override
	public Integer getNumberOfAllDocs() {
		return this.numberOfDocs;
	}
	
	public static DocumentFrequencyProviderViaMap deserializeFromFile(String filepath) throws FileNotFoundException, IOException, ClassNotFoundException{
		
		FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
                fis = new FileInputStream(filepath);
                in = new ObjectInputStream(fis);
                DocumentFrequencyProviderViaMap result = (DocumentFrequencyProviderViaMap) in.readObject();
                return result;
        } 
        finally {
            IOUtils.closeQuietly(in);
        }
	}

	public static void serialize(DocumentFrequencyProviderViaMap docFrequencyProvider, String filepath) throws FileNotFoundException, IOException, ClassNotFoundException{

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
}
