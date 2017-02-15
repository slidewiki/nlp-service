package services.nlp.languagedetection;

public interface ILanguageDetector {

	public static String valueForUnknownLanguage = "UNKNOWN";
	
	public String getLanguage(String text);
}
