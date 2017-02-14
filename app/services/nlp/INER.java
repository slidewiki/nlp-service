package services.nlp;

import opennlp.tools.util.Span; 

public interface INER {
    public Span[] getNEs(String[] tokens);
    public String getName();
}

