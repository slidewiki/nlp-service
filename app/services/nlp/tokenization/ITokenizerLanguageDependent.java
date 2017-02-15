package services.nlp.tokenization;

public interface ITokenizerLanguageDependent {
	
    String[] tokenize(String input, String language);

}
