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

public class DocFrequencyProviderViaMap implements IDocFrequencyProvider, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6555208226879791393L;
	public static String specialNameForLanguageIndependent = "ALL";
	
	private Map<String,DocFrequencyStoreViaMap> mapLanguageToDocFreqStore;
	
	public DocFrequencyProviderViaMap() {
		
		this.mapLanguageToDocFreqStore = new HashMap<>();
	}
	
	public DocFrequencyProviderViaMap(Map<String, DocFrequencyStoreViaMap> mapLanguageToDocFreqStore) {
		
		this.mapLanguageToDocFreqStore = mapLanguageToDocFreqStore;
	}

	@Override
	public Integer getDocFrequency(String term, String language) {
		
		String languageToUse;
		if(language==null){
			languageToUse = specialNameForLanguageIndependent;
		}else{
			languageToUse = language;
		}
		if(this.mapLanguageToDocFreqStore.containsKey(languageToUse)){
			
			return mapLanguageToDocFreqStore.get(languageToUse).getDocFrequency(term);
			
		}else{// given language not available
			// TODO: discuss if another default language should be used as fallback?
			return 0;
		}
	}

	@Override
	public Integer getNumberOfAllDocs(String language) {

		String languageToUse;
		if(language==null || language.length()==0){
			languageToUse = specialNameForLanguageIndependent;
		}else{
			languageToUse = language;
		}
		if(this.mapLanguageToDocFreqStore.containsKey(languageToUse)){
			return this.mapLanguageToDocFreqStore.get(languageToUse).getNumberOfAllDocs();
		}else{
			return 0;
		}
	}
	
	public void addDocument(Set<String> termtypesOfDoc, String language){
		
		String languageToUse;
		if(language==null || language.length()==0){
			languageToUse = specialNameForLanguageIndependent;
		}else{
			languageToUse = language;
		}
		if(this.mapLanguageToDocFreqStore.containsKey(languageToUse)){
			// add terms to doc store
			DocFrequencyStoreViaMap docStore = mapLanguageToDocFreqStore.get(languageToUse);
			docStore.addDocument(termtypesOfDoc);
				
		}else{
			// create new doc store with given terms and add to map
			DocFrequencyStoreViaMap docStore = new DocFrequencyStoreViaMap();
			docStore.addDocument(termtypesOfDoc);
			this.mapLanguageToDocFreqStore.put(languageToUse, docStore);
		}
	}
	
	public void removeDocument(Set<String> termtypesOfDoc, String language){
		String languageToUse;
		if(language==null || language.length()==0){
			languageToUse = specialNameForLanguageIndependent;
		}else{
			languageToUse = language;
		}
		// language not available
		if(!this.mapLanguageToDocFreqStore.containsKey(languageToUse)){
			return;
		}
		
		// get docStore for language
		DocFrequencyStoreViaMap docStore = mapLanguageToDocFreqStore.get(languageToUse);
		docStore.removeDocument(termtypesOfDoc);
		
	}
	
	
	
	
	public static DocFrequencyProviderViaMap deserializeFromFile(String filepath) throws FileNotFoundException, IOException, ClassNotFoundException{
		
		FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
                fis = new FileInputStream(filepath);
                in = new ObjectInputStream(fis);
                DocFrequencyProviderViaMap result = (DocFrequencyProviderViaMap) in.readObject();
                return result;
        } 
        finally {
            IOUtils.closeQuietly(in);
        }
	}

	public static void serialize(DocFrequencyProviderViaMap docFrequencyProvider, String filepath) throws FileNotFoundException, IOException, ClassNotFoundException{

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
	 
	public Map<String,Integer> getStatisticsOfDocsPerLanguage(){
		Set<String> languages = this.mapLanguageToDocFreqStore.keySet();
		Map<String,Integer> mapLanguagesToNumberOfDocs = new HashMap<>();
		for (String language : languages) {
			Integer numDocs = this.getNumberOfAllDocs(language);
			mapLanguagesToNumberOfDocs.put(language, numDocs);
		}
		return mapLanguagesToNumberOfDocs;
	}
	
	public String getStatisticsOfDocsPerLanguageAsString(){
		Map<String,Integer> stats = getStatisticsOfDocsPerLanguage();
		List<Entry<String,Integer>> statsSorted = Sorter.sortByValueAndReturnAsList(stats, true);
		StringBuilder sb = new StringBuilder();
		for (Entry<String,Integer> entry : statsSorted) {
			sb.append(entry.getKey() + "\t" + entry.getValue());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String getStatisticsOfDocsPerLanguageAsStringInclTopXTerms(int x, String delimiterBetweenEntries, boolean outputNumDocsForType, String delimiterBetweenTypeAndNumDocs, String delimiterBetweenAfterNumDocs){
		
		Map<String,Integer> stats = getStatisticsOfDocsPerLanguage();
		List<Entry<String,Integer>> statsSorted = Sorter.sortByValueAndReturnAsList(stats, true);
		StringBuilder sb = new StringBuilder();
		for (Entry<String,Integer> entry : statsSorted) {
			String language = entry.getKey();
			sb.append(language + "\t" + entry.getValue());
			sb.append("\ttop terms:\t");
			DocFrequencyStoreViaMap docStore = this.mapLanguageToDocFreqStore.get(language);
			String topTermsString = docStore.getTopXTypesBasedOnDocFreqAsString(x, delimiterBetweenEntries, outputNumDocsForType, delimiterBetweenTypeAndNumDocs, delimiterBetweenAfterNumDocs);
			
			sb.append(topTermsString);
			sb.append("\n");
		}
		return sb.toString();
	}
	
}
