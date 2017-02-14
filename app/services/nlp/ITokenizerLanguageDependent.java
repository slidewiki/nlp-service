package services.nlp;

public interface ITokenizerLanguageDependent {
	
    String[] tokenize(String input, String language);

}
