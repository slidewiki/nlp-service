package services.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import opennlp.tools.util.Span;

public class NLP_Component implements ITagger{

    private ITokenizer tokenizer;
    private Map<String,INER> nerMap;
//    private ITagResultSelector tagresultSelector;
    
//    @Inject
 //   public NLP_Component(ITokenizerProvider tokenizerProvider, List<INER> nerList, ITagResultSelector tagresultSelector){
        
   //     this.tokenizerProvider = tokenizerProvider;
     //   this.nerList = nerList;
        //this.tagresultSelector = tagresultSelector;

        
   // }
    
    @Inject
    public NLP_Component(ITokenizer tokenizer, Map<String,INER> nerMap){
        
        this.tokenizer = tokenizer;
        this.nerMap = nerMap;
        //this.tagresultSelector = tagresultSelector;

        
    }
    
    @Override
    public String getTagsAsString(String input){
        
        String[] tokens =  tokenizer.tokenize(input);
        StringBuilder sb = new StringBuilder();
        Set<String> keys = nerMap.keySet();
        for (String key : keys) {
			INER ner = nerMap.get(key);
			Span[] neSpans = ner.getNEs(tokens);
			sb.append(key);
			sb.append(": ");
			sb.append(NER_OpenNLP.getStringRepresentationOfSpans(tokens, neSpans, ", ", true));
            sb.append("\n");
        }
        return sb.toString();
    }

	@Override
	public List<NlpTag> getTags(String input) {
		List<NlpTag> result = new ArrayList<>();
		String[] tokens =  tokenizer.tokenize(input);
        Set<String> keys = nerMap.keySet();
        for (String key : keys) {
			INER ner = nerMap.get(key);
			Span[] neSpans = ner.getNEs(tokens);
			List<NlpTag> listCurrentNER = NlpTag.fromSpans(neSpans, tokens, key);
			result.addAll(listCurrentNER);
        }
        return result;
	}
    

}