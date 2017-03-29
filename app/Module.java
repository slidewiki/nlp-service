import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import controllers.InstanceProvider;
import controllers.NLPController;
import play.Configuration;

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
        
//        bind(ILanguageDetector.class).to(LanguageDetector_optimaize.class);
//        bind(ITokenizerLanguageDependent.class).to(TokenizerLanguageDependentViaMap.class);
//        bind(INERLanguageDependent.class).to(NERLanguageDependentViaMap.class);
//
//        bind(IHtmlToText.class).to(HTMLJsoup.class);

    }

    
    @Provides
    public NLPController provideNLPController(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException {
    	return InstanceProvider.provideNLPController(configuration);
    
    }


 
 
}
