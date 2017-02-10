package services.nlp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class Tokenizer_OpenNLP implements ITokenizer{

    private Tokenizer tokenizer;
 
    @Inject
    public Tokenizer_OpenNLP(Tokenizer tokenizer){
       this.tokenizer = tokenizer;
    }

    public Tokenizer_OpenNLP(String filepathOfModel){
      
       loadModelFromFile(filepathOfModel);
       
   }

    @Override
    public String[] tokenize(String input){
        return this.tokenizer.tokenize(input);
    }
    

    
    private void loadModelFromFile(String filepath){
    
        InputStream modelIn = null;
        try {
            modelIn = new FileInputStream(filepath);
            TokenizerModel model = new TokenizerModel(modelIn);
            this.tokenizer = new TokenizerME(model);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (modelIn != null) {
                try {
                    modelIn.close();
                }
                catch (IOException e) {
                }
            }   
        }
        
        
    }
}
