package services.nlp;

public class NlpTag {

	private String name;
	private String link;
	private double automaticImportanceValue;
	
	
	public NlpTag(String name, String link, double automaticImportanceValue) {
		super();
		this.name = name;
		this.automaticImportanceValue = automaticImportanceValue;
		this.link = link;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public double getAutomaticImportanceValue() {
		return automaticImportanceValue;
	}


	public void setAutomaticImportanceValue(double automaticImportanceValue) {
		this.automaticImportanceValue = automaticImportanceValue;
	}


	public String getLink() {
		return link;
	}


	public void setLink(String link) {
		this.link = link;
	}




	
}
