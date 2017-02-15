package services.nlp.languagedetection;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

public class LanguageDetector_optimaize implements ILanguageDetector {

	LanguageDetector languageDetector;
	TextObjectFactory textObjectFactory;

	public LanguageDetector_optimaize(LanguageDetector languageDetector, TextObjectFactory textObjectFactory) {
		
		this.languageDetector = languageDetector;
		this.textObjectFactory = textObjectFactory;
		
	}

	public LanguageDetector_optimaize() throws IOException {
		
		//load all languages:
		List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();

		//build language detector:
		this.languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
		        .withProfiles(languageProfiles)
		        .build();

		//create a text object factory
		this.textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
	}


	@Override
	public String getLanguage(String text) {

		TextObject textObject = textObjectFactory.forText(text);
		Optional<LdLocale> lang = languageDetector.detect(textObject);
		if(lang.equals(Optional.absent())){
			return ILanguageDetector.valueForUnknownLanguage;
		}
		return lang.get().getLanguage();
	}

}
