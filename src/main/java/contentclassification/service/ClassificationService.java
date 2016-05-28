package contentclassification.service;

import contentclassification.domain.*;

import java.util.List;
import java.util.Map;

/**
 * Created by rsl_prod_005 on 5/24/16.
 */
public interface ClassificationService {

    public List<String> uniqueCollection(String text);
    public String[] tokenize(String text);
    public String[] sentenceDetection(String text);
    public String getStem(String word);
    public List<String> getStems(String[] tokens);
    public List<Map> getPos(String[] tokens);
    public List<String> getIntersection(List<String> a, List<String> b);
    public List<Categories> getCategories();
    public double getTFScore(String[] document, String term);
    public double getIdfScore(String[] document, String term);
    public double getTfIdfWeightScore(String[] document, String term);
    public List<String> prepareTokens(List<String> tokens);
    public <T> List<Map> generateKeyValuePairs(List<T> object);
    public String getContentMetaDataValue(NameAndContentMetaData n, List<Map> metaList, WebMetaName webMetaName);
    public List<String> getMultiWordedAttributes(Categories categories);
    public boolean termFoundInSentences(String[] sentences, String term);
    public Integer getTermToGroupScore(String term, String group);
    public List<TermToGroupScore> getTermToGroupByContentAreaGroupings(List<TermToGroupScore> termToGroupScores,
                                                                       ContentAreaGroupings contentAreaGroupings);
}
