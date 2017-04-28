package services.nlp.microserviceutil;

/**
 * Convenience class for fast creating a DBPedia Spotlight instance with url and fallback URL. Do just use for test cases!
 * @author aschlaf
 *
 */
public class DBPediaSpotlightTestInstance {

	public static String testUrlOfDBPediaWebService = "http://model.dbpedia-spotlight.org/en/annotate";
	public static String[] testFallbackURLs = new String[]{
//			"http://model.dbpedia-spotlight.org/en/annotate",
			"http://spotlight.sztaki.hu:2222/rest/annotate", 
			"http://model.dbpedia-spotlight.org/en/annotate", 
			"http://www.dbpedia-spotlight.com/en/annotate", 
			"http://api.dbpedia-spotlight.org/rest/annotate", 
//			"http://spotlight.dbpedia.org/rest/annotate",
//			"https://github.com/dbpedia-spotlight/dbpedia-spotlightrest/annotate"
	};

	public static DBPediaSpotlightUtil getTestInstanceDBPediaSpotlightUtil(){
		return new DBPediaSpotlightUtil(testUrlOfDBPediaWebService, testFallbackURLs);
	}
}
