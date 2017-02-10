package services.nlp;

import opennlp.tools.util.Span; 

public interface INER {
    Span[] getNEs(String[] tokens);
}
