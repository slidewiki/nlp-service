package services.nlp;

import java.util.List;

public interface ITagger {
    
    List<NlpTag> getTags(String input);
}
