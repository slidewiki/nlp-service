package services.nlp.stopwords;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import services.util.FileReaderUtils;

/**
 * Util class for loading data from project Wortschatz (wortschatz.uni-leipzig.de / http://corpora.informatik.uni-leipzig.de)
 * @author aschlaf
 *
 */
public class WortschatzUtil {

	/**
	 * Loads top words of words file from Wortschatz project like available on http://corpora2.informatik.uni-leipzig.de/download.html
	 * @param filepath of the words.txt of wortschatz
	 * @param topX
	 * @param includeSpecialCharsAtBeginningAdditionalToTopX
	 * @param toLowerCase
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Set<String> loadTopWordsFormsFromWortschatzWords(String filepath, int topX, boolean includeSpecialCharsAtBeginningAdditionalToTopX, boolean toLowerCase) throws FileNotFoundException, IOException{
		Set<String> result = new HashSet<>();
		BufferedReader reader = FileReaderUtils.getReader(filepath);
		String line = null;
		int lineCounter = 0;
		while ((line = reader.readLine()) != null){
			
			String[] entries = line.split("\t");
			Integer id = Integer.valueOf(entries[0]);
			
			if(id<=100){// specialChars
				if(!includeSpecialCharsAtBeginningAdditionalToTopX){
					continue;
				}
				
			}else{// top x word words
				lineCounter++;
			}
				
			if(lineCounter > topX){
				break;
			}
			
			String wordform = entries[1]; 
			if(toLowerCase){
				wordform = wordform.toLowerCase();
			}
			result.add(wordform);
			
			
		}
		reader.close();
		return result;

	}
}
