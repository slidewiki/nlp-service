import java.util.HashMap;
import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import play.Configuration;
import services.nlp.INER;
import services.nlp.ITagger;
import services.nlp.ITokenizer;
import services.nlp.NER_OpenNLP;
import services.nlp.NLP_Component;
import services.nlp.Tokenizer_OpenNLP;

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
public class Module extends AbstractModule {

    @Override
    public void configure() {
        
        //bind(ITagger.class).to(Tokenizer_OpenNLP.class);
        bind(ITokenizer.class).to(Tokenizer_OpenNLP.class);
        bind(ITagger.class).to(NLP_Component.class);
    
    }


    @Provides
    public Tokenizer_OpenNLP provideTokenizerOpenNLP(Configuration configuration) {
        
        String filepathModel = configuration.getString("tokenizer.opennlp.model.filepath");
        Tokenizer_OpenNLP tokenizerOpenNLP= new Tokenizer_OpenNLP(filepathModel);
        return tokenizerOpenNLP;
    }
    
     @Provides
    public Map<String,INER> provideNERMap(Configuration configuration) {
        
        System.out.println("calling NER map provider");
        Map<String,INER> map = new HashMap<>();
        
        // TODO: make configurable
        map.put("openNLP-en-person", new NER_OpenNLP("resources/opennlp/en-ner-person.bin"));
        map.put("openNLP-en-organisation", new NER_OpenNLP("resources/opennlp/en-ner-organization.bin"));
        map.put("openNLP-en-location", new NER_OpenNLP("resources/opennlp/en-ner-location.bin"));
        map.put("openNLP-es-person", new NER_OpenNLP("resources/opennlp/es-ner-person.bin"));
        map.put("openNLP-es-organisation", new NER_OpenNLP("resources/opennlp/es-ner-organization.bin"));
        map.put("openNLP-es-location", new NER_OpenNLP("resources/opennlp/es-ner-location.bin"));
        map.put("openNLP-nl-person", new NER_OpenNLP("resources/opennlp/nl-ner-person.bin"));
        map.put("openNLP-nl-organisation", new NER_OpenNLP("resources/opennlp/nl-ner-organization.bin"));
        map.put("openNLP-nl-location", new NER_OpenNLP("resources/opennlp/nl-ner-location.bin"));

        
        System.out.println("\t ner map size " + map.size());

        return map;
    }
    
     @Provides
     public Map<String,ITokenizer> provideTokenizerMap(Configuration configuration) {
         
         System.out.println("calling tokenizer map provider");
         Map<String,ITokenizer> map = new HashMap<>();
         
         // TODO: make configurable
         map.put("en", new Tokenizer_OpenNLP("resources/opennlp/en-token.bin"));
         map.put("de", new Tokenizer_OpenNLP("resources/opennlp/de-token.bin"));
  
         
         System.out.println("\t token map size " + map.size());

         return map;
     }

}
