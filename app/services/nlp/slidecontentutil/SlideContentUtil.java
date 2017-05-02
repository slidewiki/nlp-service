package services.nlp.slidecontentutil;

import com.fasterxml.jackson.databind.JsonNode;

import services.nlp.html.IHtmlToText;
import services.nlp.microserviceutil.DeckServiceUtil;

public class SlideContentUtil {

	public static String retrieveSlideTitleAndTextWithoutHTML(IHtmlToText htmlToPlainText, JsonNode slide, String separatorToUseBetweenSlideTitleAndSlideContent){
		// slide title
		String slidetitle = htmlToPlainText.getText(DeckServiceUtil.getSlideTitle(slide));
		slidetitle =	normalizeSlideTitle(slidetitle);
		String slideTitleAndText = "";
		if(slidetitle.length()>0 ){
			slideTitleAndText = slidetitle;
		}
		// slide content without html
		String contentWithoutHTML = htmlToPlainText.getText(DeckServiceUtil.getSlideContent(slide));
		contentWithoutHTML = normalizeSlideContent(contentWithoutHTML);

		// whole slide text (title & content without html)
		if(contentWithoutHTML.length()>0){
			if(slideTitleAndText.length()==0){
				slideTitleAndText = contentWithoutHTML;
			}else{
				slideTitleAndText = slideTitleAndText + separatorToUseBetweenSlideTitleAndSlideContent + contentWithoutHTML;
			}
		}
		return slideTitleAndText;

	}
	
	/**
	 * Removes such text like "New slide" or "No title"
	 * @return
	 */
	public static String normalizeSlideTitle(String input){
		String result = input;
		String[] thingsToRemove = new String[]{"no title", "No title", "new slide", "New slide", "\n"};
		for (String stringToRemove : thingsToRemove) {
			result = result.replace(stringToRemove, " ").trim();
		}
		return result;
	}

	/**
	 * TODO: make this more general - recheck with default values used when empty / default slides are created
	 * @param input
	 * @return
	 */
	public static String normalizeSlideContent(String input){
		String result = input;
		String[] thingsToRemove = new String[]{"Text bullet 1", "Text bullet 2", "Text bullet 3", "Text bullet 4", "Bullet 1", "Bullet 2", "Bullet 3", "Bullet 4"};
		for (String stringToRemove : thingsToRemove) {
			result = result.replace(stringToRemove, " ").trim();
		}
		String [] thingstoReplaceByNewLine = new String[]{"•"};
		for (String stringToReplace : thingstoReplaceByNewLine) {
			if(result.contains(stringToReplace)){
				result = result.replace(stringToReplace, " \n").trim();

			}
		}
		
		return result.trim();
	}

}
