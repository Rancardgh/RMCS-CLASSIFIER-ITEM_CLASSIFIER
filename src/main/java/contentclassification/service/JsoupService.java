package contentclassification.service;

import contentclassification.domain.AppUtils;
import contentclassification.domain.HtmlUnitImpl;
import contentclassification.domain.JsoupImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rsl_prod_005 on 5/6/16.
 */
@Service
public class JsoupService {
    private static final Logger logger = LoggerFactory.getLogger(JsoupService.class);

    public String getTitle(String url){
        String title = null;
        try {
            JsoupImpl jsoup = JsoupImpl.setUrl(url);
            title = jsoup.getTitle();
        } catch (Exception e){
            logger.debug("Error in getting title. Message:" + e.getMessage());
        }
        return title;
    }

    public Document getDocument(String url){
        Document document = null;
        try{
            JsoupImpl jsoup = JsoupImpl.setUrl(url);
            document = jsoup.getDocument();
        } catch (Exception e){
            logger.debug("Error in getting document. Message: "+ e.getMessage());
        }
        return document;
    }

    public Document getDocumentByParser(String htmlCode){
        Document document = null;
        try {
            document = JsoupImpl.parseHtml(htmlCode);
        } catch (Exception e){
            logger.debug("Error in parsing html from content as string. Message: "+ e.getMessage());
        }
        return document;
    }

    public List<String> metas(String url){
        List<String> metas = null;
        try{
            JsoupImpl jsoup = JsoupImpl.setUrl(url);
            metas = jsoup.getMeta();
        } catch (Exception e){
            logger.debug("Error in getting meta details. Message: "+ e.getMessage());
        }
        return metas;
    }


    public List<String> metasByDocument(Document document){
        List<String> metas = null;
        try{
            if (document != null) {
                metas = new ArrayList<>();
                Elements elements = document.select("meta");
                if (!elements.isEmpty()) {
                    for (Element element : elements) {
                        metas.add(element.toString());
                    }
                }
            }
        } catch (Exception e){
            logger.debug("Error in getting meta details. Message: "+ e.getMessage());
        }
        return metas;
    }


    public String getTitleByDocument(Document document){
        String title = null;
        try {
            title = document.title();
        } catch (Exception e){
            logger.debug("Error in getting title. Message:" + e.getMessage());
        }
        return title;
    }
    public List<String> links(String url){
        List<String> links = null;
        try{
            JsoupImpl jsoup = JsoupImpl.setUrl(url);
            links = jsoup.getLinks();
        } catch (Exception e){
            logger.debug("Error in getting links. Message: "+ e.getMessage());
        }
        return links;
    }

    public String bodyText(String url){
        String text = null;
        try{
            JsoupImpl jsoup = JsoupImpl.setUrl(url);
            text = jsoup.getBody();
        } catch (Exception e){
            logger.debug("Error in getting body. Message: "+ e.getMessage());
        }
        return text;
    }

    public String bodyTextByHtmlUnit(String url){
        String text = null;
        try {
            HtmlUnitImpl htmlUnit = HtmlUnitImpl.setUrl(AppUtils.getUrl(url));
            text = htmlUnit.getText();
        } catch (Exception e){
            logger.debug("Error in getting text using. Message: "+ e.getMessage());
        }
        return text;
    }

    public String parseHtmlText(String html, String url){
        String text = null;
        try{
            HtmlUnitImpl htmlUnit = HtmlUnitImpl.setUrl(AppUtils.getUrl(url));
            text = htmlUnit.parseHTMLText(html, url);
        } catch (Exception ex){
            logger.debug("Error in getting text by parsing html text: "+ ex.getMessage());
        }
        return text;
    }

    public String getContentAsString(String url){
        String text = null;
        try{
            HtmlUnitImpl htmlUnit = HtmlUnitImpl.setUrl(AppUtils.getUrl(url));
            text = htmlUnit.getContentAsString();
        } catch (Exception e){
            logger.debug("Error in getting content string. Message: "+ e.getMessage());
        }
        return text;
    }

    public List<Map> getLinksUrlAndValue(String url){
        List<Map> links = null;
        try{
            JsoupImpl jsoup = JsoupImpl.setUrl(url);
            links = jsoup.getLinksUrlAndValue();
        } catch (Exception e){
            logger.debug("Error in getting links. Message: "+ e.getMessage());
        }
        return links;
    }

    public List<Map> getLinksUrlAndValueByDocument(Document document){
        List<Map> links = new ArrayList<>();
        try {
            Elements elementLinks = document.select("a[href]");
            if (!elementLinks.isEmpty()) {
                for (Element link : elementLinks) {
                    Map<String, String> m = new HashMap<>();
                    m.put("value", link.text());
                    m.put("link", link.attr("abs:href"));
                    links.add(m);
                }
            }
        } catch (Exception e){
            logger.debug("Error in getting URLs : %s", e.getMessage());
        }
        return links;
    }

    public List<String> getAllTags(String url){
        List<String> tags = new ArrayList<>();
        try{
            JsoupImpl jsoup = JsoupImpl.setUrl(url);
            tags.addAll(jsoup.getAllTags());
        } catch (Exception e){
            logger.debug("Error in getting tags. Message: "+ e.getMessage());
        }
        return tags;
    }
}
