package contentclassification.service;

import contentclassification.domain.Categories;

import java.util.List;
import java.util.Map;

/**
 * Created by rsl_prod_005 on 5/24/16.
 */
public interface ClassificationService {

    public List<String> uniqueCollection(String text);
    public String[] tokenize(String text);
    public String getStem(String word);
    public List<String> getStems(String[] tokens);
    public List<Map> getPos(String[] tokens);
    public List<String> getIntersection(List<String> a, List<String> b);
    public List<Categories> getCategories();
    public double getTFScore(String[] document, String term);
    public double getIdfScore(String[] document, String term);
    public double getTfIdfWeightScore(String[] document, String term);
}