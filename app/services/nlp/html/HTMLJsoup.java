package services.nlp.html;

import org.jsoup.Jsoup;

public class HTMLJsoup implements IHtmlToText{

	@Override
	public String getText(String html) {
		String text = Jsoup.parse(html).text();
		// do this parsing again because for old platform e.g. for deck id 149, slide id 2380-3 the text wasn't correctly extracted
		String result = Jsoup.parse(text).text();
		
		return result;
	}

}
