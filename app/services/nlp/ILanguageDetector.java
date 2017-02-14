package services.nlp;

public interface ILanguageDetector {

	public static String valueForUnknownLanguage = "UNKNOWN";
	
	public String getLanguage(String text);
}
