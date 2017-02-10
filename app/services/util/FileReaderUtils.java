package services.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;

public class FileReaderUtils {
	
	/**
	 * Returns all files from a given directory.
	 * @param directory The directory
	 * @return a list of files
	 */
	public static List<File> getFilesFromDirectory(File directory, boolean sortByName) {

		IOFileFilter fileFilter = FileFilterUtils.makeSVNAware(HiddenFileFilter.VISIBLE);
	    IOFileFilter dirFilter = FileFilterUtils.makeSVNAware(FileFilterUtils.directoryFileFilter());
	    List<File> result = new ArrayList<File>(FileUtils.listFiles(directory, fileFilter, dirFilter));
	    if(sortByName){
	    	Collections.sort(result);
	    }
	    
	    return result;
	}

	public static List<String[]> readColumns(String filepath, String columnSeparator, boolean hasHeadingToIgnore) throws IOException{
		
		List<String[]> result = new ArrayList<String[]>();
		BufferedReader reader = getReader(filepath);
		String line;
		int counterLine = 0;
		while ((line = reader.readLine()) != null) {
			counterLine++;
			if(counterLine == 1 && hasHeadingToIgnore){
				continue;
			}			
			String[] columns = line.split("\t");
			result.add(columns);
		}
		reader.close();
		return result;
	}
	
	
	public static List<String> readSpecificColumnContentAsList(String filepath, String columnSeparator, int columnToExtract, boolean hasHeadingToIgnore) throws IOException{
		
		List<String> result = new ArrayList<String>();
		String line;
		int counterLine = 0;
		BufferedReader reader = getReader(filepath);
		while ((line = reader.readLine()) != null) {
			counterLine++;
			if(counterLine == 1 && hasHeadingToIgnore){
				continue;
			}			
			String[] columns = line.split("\t");
			result.add(columns[columnToExtract]);
		}
		reader.close();
		return result;
	}

	public static List<String> readLinesToList(String filepath, boolean ignoreTrimmedEmptyLines, boolean trimLinesBeforeAdding) throws IOException{
		
		List<String> result = new ArrayList<String>();
		String line;
		BufferedReader reader = getReader(filepath);
		while ((line = reader.readLine()) != null) {
			
			if(line.trim().length() == 0){
				continue;
			}
			if(trimLinesBeforeAdding){
				result.add(line.trim());
			}else{
				result.add(line);
			}
		}
		reader.close();
		return result;
	}
	public static String[] readLinesToStringArray(String filepath, boolean ignoreTrimmedEmptyLines, boolean trimLinesBeforeAdding) throws IOException{
		
		List<String> resultList = readLinesToList(filepath, ignoreTrimmedEmptyLines, trimLinesBeforeAdding);
		String[] result = new String[resultList.size()];
		resultList.toArray(result);
		return result;
	}
	/**
	 * Returns a Map containing keys and values according to the given parameters retrieved from the file having columns separated by tabulator 
	 * @param filepathMapping The file path
	 * @param columnOriginal The column number of the key for the map
	 * @param columnMappingEntry The column number of the value of the map
	 * @param hasHeadingToIgnore If true, the first line of the file is ignored not not put tp the map.
	 * @return a Map containing keys and values according to the given parameters retrieved from the file having columns separated by tabulator 
	 * @throws IOException
	 */
	public static Map<String, String> getMappping(String filepathMapping, int columnOriginal, int columnMappingEntry, boolean hasHeadingToIgnore) throws IOException{
		
		Map<String, String> mapping = new HashMap<String, String>();
		BufferedReader reader = getReader(filepathMapping);
		String line;
		int counterLine = 0;
		while ((line = reader.readLine()) != null) {
			
			counterLine++;
			if(counterLine == 1 && hasHeadingToIgnore){
				continue;
			}
			
			String[] columns = line.split("\t");
			if(columnOriginal >= columns.length){
				reader.close();
				throw new IllegalArgumentException("The column for original does not exist for line " + counterLine);
			}
			if(columnMappingEntry >= columns.length){
				reader.close();
				throw new IllegalArgumentException("The column for the mapping does not exist for line " + counterLine);
			}
				
			String original = columns[columnOriginal];
			String mappingEntry = columns[columnMappingEntry];
			if(mappingEntry.length()==0){
				// no value available for key
				continue;
			}
			mapping.put(original, mappingEntry);
		}
		reader.close();
		
		return mapping;
	}
	

	
	public static Map<String, Set<String>> getMapping(String filepathMapping, int columnOriginal, int columnMappingEntry, String separatorMultipleMappingEntries, boolean hasHeadingToIgnore) throws IOException{
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		Map<String, String> map = getMappping(filepathMapping, columnOriginal, columnMappingEntry, hasHeadingToIgnore);
		Set<String> keys = map.keySet();
		for (String key : keys) {
			
			String value = map.get(key);
			if(value.contains(separatorMultipleMappingEntries)){
				String[] values = value.split(separatorMultipleMappingEntries);
				Set<String> valueSet = new HashSet<String>();
				for (String splittedValue : values) {
					valueSet.add(splittedValue.trim());
				}
				
				result.put(key, valueSet);
			}else{
				Set<String> valueSet = new HashSet<String>();
				valueSet.add(value);
				result.put(key, valueSet);
			}
		}
		return result;

	}
	/**
	 * Returns a reverse mapping.
	 * E.g. converts from (key1->"value1,value2,value3"; key2->"value4,value5") to (value1->key1; value2->key1; value3->key1; value4->key2; value5->key2) having "," as separator.
	 * @param filepathMapping
	 * @param columnOriginal
	 * @param columnMappingEntry
	 * @param separatorMultipleMappingEntries
	 * @param hasHeadingToIgnore
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> getReverseMapping(String filepathMapping, int columnOriginal, int columnMappingEntry, String separatorMultipleMappingEntries, boolean hasHeadingToIgnore) throws IOException{
		
		Map<String,String> result = new HashMap<String, String>();
		Map<String, String> map = getMappping(filepathMapping, columnOriginal, columnMappingEntry, hasHeadingToIgnore);
		Set<String> keys = map.keySet();
		for (String key : keys) {
			
			String value = map.get(key);
			if(value.contains(separatorMultipleMappingEntries)){
				String[] values = value.split(separatorMultipleMappingEntries);
				for (String splittedValue : values) {
					result.put(splittedValue.trim(), key);
				}
				
			}else{
				result.put(value, key);
			}
		}
		return result;
	}
	
	public static BufferedReader getReader(String filepath) throws FileNotFoundException, IOException{
		BufferedReader reader;
		File currentFile = new File(filepath);
	    InputStream inputStream;
	    String path = currentFile.getAbsolutePath();
		if(path.endsWith(".gz")){
		    	inputStream = new GZIPInputStream(new FileInputStream(currentFile));
		    	reader = new BufferedReader(new InputStreamReader(inputStream));
		 }else if(path.endsWith(".tgz")){
//			 throw new IllegalArgumentException("TGZ is not supported");
//			 TarArchiveInputStream tarInput =  new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(filepath)));
//			 TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
//			 File f = currentEntry.getFile();
//			 inputStream = new FileInputStream(f);
//			 tarInput.close();
//			 inputStream = new GZIPInputStream(new TarArchiveInputStream(new FileInputStream (new File(filepath))));
			 
			
			TarArchiveInputStream tarInput = new TarArchiveInputStream(new FileInputStream(filepath));
			TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
			File inputFile = null;
			while(currentEntry != null) {
				  inputFile = currentEntry.getFile();
			}
			tarInput.close();
			reader = new BufferedReader(new FileReader(inputFile));

		 }else{
		 
		     inputStream = new FileInputStream(currentFile);
		     reader = new BufferedReader(new InputStreamReader(inputStream));
		  }
		
		return reader;
	}

	public static void serialize(Object o, String filepath) throws IOException{
		
			FileOutputStream fileOut =  new FileOutputStream(filepath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(o);
			out.close();
			fileOut.close();
	}
	
	public static Object deserialize(String filepath) throws IOException, ClassNotFoundException{
		
		FileInputStream fileIn = new FileInputStream(filepath);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        Object o = in.readObject();
        in.close();
        fileIn.close();
        return o;
	}
}
