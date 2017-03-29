package services.nlp.stopwords;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import services.util.FileReaderUtils;

public class StopwordRemoverFactory {

	/**
	 * Assumes that only wortschatz word files are contained in folder, no other files. Assumes file names start with language in ISO 639-2 (e.g. deu_wikipedia_2016_3M-words))
	 * @param folderpath
	 * @param topX
	 * @param includeSpecialCharsAtBeginningAdditionalToTopX
	 * @param toLowerCase
	 * @param filepathOfLanguageCodes
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static IStopwordRemover getStopwordRemoverFromWortschatzData(String folderpath, int topX, boolean includeSpecialCharsAtBeginningAdditionalToTopX, boolean toLowerCase, String filepathOfLanguageCodes) throws FileNotFoundException, IOException{
		
		// get mapping language codes
		Map<String,String> languageCodeMappings = FileReaderUtils.getMappping(filepathOfLanguageCodes, 4, 3, true);

		Map<String, Set<String>> mapLanguageToStopwords = new HashMap<>();
		List<File> files = FileReaderUtils.getFilesFromDirectory(new File(folderpath), false);
		for (File file : files) {
			
			// get stopwords from file
			String filepath = file.getAbsolutePath();
			Set<String> stopwords = WortschatzUtil.loadTopWordsFormsFromWortschatzWords(filepath, topX, includeSpecialCharsAtBeginningAdditionalToTopX, toLowerCase);

			// retrieve language
			// get language from filename (file name starts with language code in ISO 639-2, e.g. deu_wikipedia_2016_3M-words
			String filename = file.getName();
			String language_ISO639_2 = filename.substring(0, 3);
			// translate to ISO ISO 639-1 (e.g. "de")
			if(!languageCodeMappings.containsKey(language_ISO639_2)){
				throw new IllegalArgumentException("Could not retrieve ISO 639-1 language for language " + language_ISO639_2 + " (language retrieved from filename " + filename + ")");
			}
			String language = languageCodeMappings.get(language_ISO639_2); // _ISO639_1
			
			// store stopwords for language in map
			mapLanguageToStopwords.put(language, stopwords);
		}
		
		return new StopwordRemoverViaMap(mapLanguageToStopwords);
	}
	
	
}
