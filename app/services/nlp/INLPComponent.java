package services.nlp;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface INLPComponent {

	public ObjectNode performNLP(String input, ObjectNode node);
}
