package services.nlp.html;

import org.jsoup.Jsoup;

public class HTMLJsoup implements IHtmlToText{

	@Override
	public String getText(String html) {
		return Jsoup.parse(html).text();
	}

}
