package contentclassification.domain;

import com.google.common.collect.Sets;
import contentclassification.utilities.BM25;
import info.debatty.java.stringsimilarity.Jaccard;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rsl_prod_005 on 5/9/16.
 */
public class Classification {
    private static final Logger logger = LoggerFactory.getLogger(Classification.class);
    private String title;
    private String uri;
    private String description;

    public Classification(String title){
        this.title = title;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, Object> getClassification(){
        Map<String, Object> results = new HashMap<>();
        List<String> items = uniqueCollection();
        if(!items.isEmpty()){
            List<String> attribute = new ArrayList<>();

            for(String i : items){
                String category = getCategory(i);

                if(StringUtils.isNotBlank(category)){
                    results.put("category", category);
                }

                String color = getColor(i);
                if(StringUtils.isNotBlank(color)){
                    results.put("color", color);
                }

                List<String> a = getAttribute(category, i);
                if(a != null && !a.isEmpty()) {
                    attribute.addAll(a);
                }
            }

            //Analyze whole sentence
            if(!results.containsKey("category")){
                Categories getCategoryBySentenceComparisonResponse = getCategoryBySentenceComparison();
                if(getCategoryBySentenceComparisonResponse != null) {
                    results.put("category", getCategoryBySentenceComparisonResponse.getCategory());
                }
            }

            if(!results.containsKey("attribute")){

            }

            if(!attribute.isEmpty()){
                results.put("attribute", attribute);
                LearningImpl.generateSentence(attribute.toArray(new String[attribute.size()]));
            }

            //Search for colors.
            if(!results.containsKey("color")){
                Map<String, Object> tagToScore = new HashMap<>();
                Dataset data = new DefaultDataset();

                for(String s : items) {
                    JenaImpl jena = JenaImpl.setQuery(s);
                    List<Map> response = jena.getResponse();
                    String abstractResponse = null;

                    if (response != null && !response.isEmpty()) {
                        for (Map m : response) {
                            if (m.containsKey(RDFProperties.ABSTRACT.toString())) {
                                abstractResponse = m.get(RDFProperties.ABSTRACT.toString()).toString();
                            }
                        }
                    }

                    if (StringUtils.isNotBlank(abstractResponse)) {
                        List<String> list = loadColorPhrases();
                        String[] responseToStr = abstractResponse.replace("\r\n", " ").replace("\n", " ").split(" ");
                        if(!list.isEmpty()) {
                            double scores[] = new double[list.size()];
                            int x = 0;
                            for(String phrase : list) {
                                BM25 bm25 = new BM25(responseToStr, phrase);
                                double tf = bm25.tf();
                                scores[x] = tf;
                                x++;
                            }

                            Instance instance = new DenseInstance(scores, s);
                            data.add(instance);
                            tagToScore.put(s, scores);
                        }
                    }
//                    Clusterer clusterer = new KMeans();
//                    Dataset[] dataSets = clusterer.cluster(data);
                }

                if(!tagToScore.isEmpty()){

                }
                logger.debug("Jena response: ");
            }
        }
        return results;
    }

    public List<String> uniqueCollection(){
        List<String> titleAsList = new LinkedList<>();
        List<String> tokensIncludeWhitespaces = Arrays.asList(title.replaceAll("\\|", " ").replaceAll(" - ", " ")
                .split(" "));

        if(!tokensIncludeWhitespaces.isEmpty()) {
            for(String s : tokensIncludeWhitespaces) {
                if(StringUtils.isNotBlank(s)) {
                    titleAsList.add(s);
                }
            }
        }
        //Remove any duplicates in words.
        Set<String> unique = new HashSet<>();
        unique.addAll(titleAsList);
        titleAsList.clear();
        titleAsList.addAll(unique);
        return titleAsList;
    }

    public String[] getTokens(){
        String[] tokens = new String[]{};
        try{
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream modelStream = classLoader.getResourceAsStream("en-token.bin");
            if(modelStream != null) {
                TokenizerModel tokenizerModel = new TokenizerModel(modelStream);
                Tokenizer tokenizer = new TokenizerME(tokenizerModel);
                tokens = tokenizer.tokenize(title);
                modelStream.close();
            }
        } catch (FileNotFoundException e){
            logger.debug("File not found exception: "+ e.getMessage());
        } catch (InvalidFormatException e){
            logger.debug("Invalid format exception: "+ e.getMessage());
        } catch (IOException e){
            logger.debug("IO exception: "+ e.getMessage());
        }
        return tokens;
    }

    public String[] getSentences(){
        String[] sentences = new String[]{};
        try{
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream modelStream = classLoader.getResourceAsStream("en-sent.bin");
            if(modelStream != null) {
                SentenceModel sentenceModel = new SentenceModel(modelStream);
                SentenceDetectorME sentenceDetectorME = new SentenceDetectorME(sentenceModel);
                sentences = sentenceDetectorME.sentDetect(this.title);
                modelStream.close();
            }
        } catch (FileNotFoundException e){
            logger.debug("File not found exception: "+ e.getMessage());
        } catch (InvalidFormatException e){
            logger.debug("Invalid format exception: "+ e.getMessage());
        } catch (IOException e){
            logger.debug("IO exception: "+ e.getMessage());
        }
        return sentences;
    }

    public double[] getSentenceProbabilities(){
        double[] sentences = null;
        try{
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream modelStream = classLoader.getResourceAsStream("en-sent.bin");
            if(modelStream != null) {
                SentenceModel sentenceModel = new SentenceModel(modelStream);
                SentenceDetectorME sentenceDetectorME = new SentenceDetectorME(sentenceModel);
                sentences = sentenceDetectorME.getSentenceProbabilities();
                modelStream.close();
            }
        } catch (FileNotFoundException e){
            logger.debug("File not found exception: "+ e.getMessage());
        } catch (InvalidFormatException e){
            logger.debug("Invalid format exception: "+ e.getMessage());
        } catch (IOException e){
            logger.debug("IO exception: "+ e.getMessage());
        }
        return sentences;
    }

    private String getCategory(String attribute){
        String category = null;
        List<Categories> categories = Categories.loadCategories();
        if(StringUtils.isNotBlank(attribute)){
            if(!categories.isEmpty()) {
                for (Categories c : categories) {
                    List<String> attributes = c.getAttributes();
                    if(attributes.contains(attribute.toLowerCase().trim())){
                        category = c.getCategory();
                    }
                }
            }
        }
        return category;
    }

    private static Set<String> getCategoryByAttribute(String attribute){
        Set<String> category = new HashSet<>();
        List<Categories> categories = Categories.loadCategories();
        if(StringUtils.isNotBlank(attribute)){
            if(!categories.isEmpty()) {
                for (Categories c : categories) {
                    List<String> attributes = c.getAttributes();
                    if(attributes.contains(attribute.toLowerCase().trim())){
                        category.add(c.getCategory());
                    }

                    if(category.isEmpty()){
                        if(!attributes.isEmpty()){
                            for(String attr : attributes){
                                double score = similarityMeasurement(attr.toLowerCase().trim(),
                                        attribute.toLowerCase().trim());
                                if(score >= 0.3d) {
                                    category.add(c.getCategory());
                                }
                            }
                        }
                    }
                }
            }
        }
        return category;
    }

    public static Set<String> getAttributes(Set<String> categories, List<String> collection){
        Set<String> attributes = new HashSet<>();
        List<Categories> catList = new ArrayList<>();

        if(categories != null && !categories.isEmpty()){
            for(String c : categories){
                Categories cat = Categories.getCategoryByName(c);
                if(cat != null){
                    catList.add(cat);
                }
            }
        }

        if(!catList.isEmpty() && collection != null && !collection.isEmpty()){
            for(String q : collection) {
                for (Categories c : catList) {
                    List<String> catAttributes = c.getAttributes();
                    if(catAttributes.contains(q)){
                        attributes.add(q);
                    }

                    if(attributes.isEmpty()){
                        for(String a : catAttributes){
                            double score = similarityMeasurement(a, q.toLowerCase().trim());
                            if(score >= 0.3d){
                                if(!q.equalsIgnoreCase(a)) {
                                    attributes.add(q.toLowerCase());
                                } else {
                                    attributes.add(a);
                                }
                            }
                        }
                    }
                }
            }
        }

        return attributes;
    }

    private String getColor(String attribute){
        String color = null;
        List<Color> colors = Color.loadColors();
        if(StringUtils.isNotBlank(attribute)){
            if(!colors.isEmpty()){
                for(Color c : colors) {
                    if (c.getName().equalsIgnoreCase(attribute)) {
                        color = c.getName();
                    }
                }
            }
        }
        return color;
    }

    private List<String> getAttribute(String category, String attribute){
        List<String> proposed = new ArrayList<>();
        List<Categories> categories = Categories.loadCategories();
        if(StringUtils.isNotBlank(attribute)){
            if(!categories.isEmpty()){
                for(Categories c : categories){
                    if(c.getCategory().equalsIgnoreCase(category)){
                        List<String> attributes = c.getAttributes();
                        if(!attributes.isEmpty()){
                            for(String a : attributes) {
                                if(!a.equalsIgnoreCase(category)) {
                                    int distance = StringUtils.getLevenshteinDistance(attribute, a);
                                    if (distance >= 0 && distance < 3) {
                                        proposed.add(attribute.toLowerCase().trim());
                                        //                                if (attributes.contains(attribute.toLowerCase().trim())) {
                                        //                                    proposed = attribute.toLowerCase().trim();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(!proposed.isEmpty()){
            Set<String> cleanUp = new HashSet<>();
            cleanUp.addAll(proposed);
            proposed.clear();
            proposed.addAll(cleanUp);
        }
        return proposed;
    }

    public List<String> generateTags(){
        List<String> tags = new ArrayList<>();
        Map<String, Object> classifications = this.getClassification();
        if(classifications != null && !classifications.isEmpty()){
            Set<String> cleaner = new HashSet<>();
            if(classifications.containsKey("category")){
                String category = classifications.get("category").toString();
                if(StringUtils.isNotBlank(category)){
                    cleaner.add(category);
                }
            }
            tags.addAll(cleaner);
        }
        return tags;
    }

    public Categories getCategoryBySentenceComparison(){
        Categories categories = null;
        String title = this.getTitle();
        if(StringUtils.isNotBlank(title)) {
            int response = StringUtils.getLevenshteinDistance(title, "ruffle front");
            int d = "ruffle front".length();
            Object h = (1 - response) / Math.max(title.length(), d);

            List<Categories> categoryList = Categories.loadCategories();
            if (categoryList != null && !categoryList.isEmpty()) {
                for (Categories c : categoryList) {
                    List<String> attributes = c.getAttributes();
                    if (!attributes.isEmpty())
                        for (String attribute : attributes) {
                            double stringCompare = StringCompare.compareStrings(title.toLowerCase(), attribute);
                            if(stringCompare >= 0.50D) {
                                if(title.toLowerCase().contains(attribute)) {
                                    categories = c;
                                }
                            }
                        }
                }
            }
        }

        return categories;
    }

    public static Map<String, Object> getClassification(List<String> collection){
        Map<String, Object> results = new HashMap<>();
        if(!collection.isEmpty()){
            Set<String> categories = new HashSet<>();
            for(String i : collection){
                categories.addAll(getCategoryByAttribute(i));
                if(!categories.isEmpty()){
                    results.put("categories", categories);
                }
            }

            Set<String> attributes = getAttributes(categories, collection);
            if(!attributes.isEmpty()){
                results.put("attributes", attributes);
            }
        }
        return results;
    }

    public static double similarityMeasurement(String str1, String str2){
        double answer = 0;
        if (StringUtils.isNotBlank(str1) && StringUtils.isNotBlank(str2)) {
            Jaccard jaccard = new Jaccard();
            answer = jaccard.similarity(str1, str2);
        }
        return answer;
    }

    public static List<String> loadColorPhrases(){
        List<String> colorPhrases = new ArrayList<>();
        try {
            Yaml yaml = new Yaml();
            colorPhrases.addAll((List<String>) yaml.load(new FileInputStream(new File("color-phrase"))));
        } catch (Exception e){
            logger.debug("Error in getting color phrases. Message: "+ e.getMessage());
        }
        return colorPhrases;
    }

    public List<Map> getPos(String[] tokens){
        List<Map> pos = new ArrayList<>();
        if(tokens != null && tokens.length > 0){
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("en-pos-maxent.bin");
            if(inputStream != null ) {
                try {
                    Path temp = Files.createTempFile("en-pos-maxent", ".bin");
                    Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
                    File file = temp.toFile();

                    if(file.exists() && file.canRead()) {
                        POSModel posModel = new POSModelLoader().load(file);
                        POSTaggerME posTaggerME = new POSTaggerME(posModel);
                        String[] tags = posTaggerME.tag(tokens);
                        int x = 0;
                        for (String t : tags) {
                            try {
                                Map<String, Object> map = new HashMap<>();
                                map.put("token", tokens[x]);
                                map.put("pos", t);
                                POSRESPONSES posresponses = POSRESPONSES.valueOf(t);
                                map.put("initial", posresponses != null ? posresponses.toString() : "");
                                pos.add(map);
                                x++;
                            } catch (Exception e){
                                logger.warn("Error in getting enum for parts-of-speech.  Term : "+ t +" Message: "+ e.getMessage());
                            }
                        }
                    } else {
                        logger.info("en-pos-maxent.bin file not found or is not readable.");
                    }
                } catch (Exception e){
                    logger.debug("IO Exception: "+ e.getMessage());
                } finally {
                    try {
                        inputStream.close();
                    } catch (Exception e){
                        logger.warn("Error in closing file. Message : "+ e.getMessage());
                    }
                }
            }
        }
        return pos;
    }

    public List<Map> getPos(String[] tokens, List<POSRESPONSES> posresponsesList){
        List<Map> pos = new ArrayList<>();
        if(tokens != null && tokens.length > 0){
            List<Map> allPos = getPos(tokens);
            if(!allPos.isEmpty()){
                for(Map mapPos : allPos){
                    if(mapPos.containsKey("pos")){
                        POSRESPONSES posresponses = POSRESPONSES.valueOf(mapPos.get("pos").toString());
                        if(posresponsesList.contains(posresponses)){
                            pos.add(mapPos);
                        }
                    }
                }
            }
        }
        return pos;
    }

    public <T> List<T> union(List<T> a, List<T> b){
        Set<T> set = new HashSet<>();
        set.addAll(a);
        set.addAll(b);
        return new ArrayList<>(set);
    }

    public <T> List<T> intersection(List<T> a, List<T> b){
        List<T> intersect = new ArrayList<>();
//        b.retainAll(a);
//        intersect.addAll(b);
        Set<T> setA = Sets.newHashSet(a);
        Set<T> setB = Sets.newHashSet(b);
        Set<T> inter = Sets.intersection(setA, setB);
        intersect.addAll(inter);
        return intersect;
    }

    public static <T> List<T> getIntersection(List<T> a, List<T> b){
        List<T> intersect = new ArrayList<>();
//        b.retainAll(a);
//        intersect.addAll(b);
        Set<T> setA = Sets.newHashSet(a);
        Set<T> setB = Sets.newHashSet(b);
        Set<T> inter = Sets.intersection(setA, setB);
        intersect.addAll(inter);
        return intersect;
    }

    public static <T> List<T> getUnion(List<T> a, List<T> b){
        List<T> intersect = new ArrayList<>();
        Set<T> setA = Sets.newHashSet(a);
        Set<T> setB = Sets.newHashSet(b);
        Set<T> inter = Sets.union(setA, setB);
        intersect.addAll(inter);
        return intersect;
    }

    public static List<FabricName> isFabricPresent(String sentence){
        List<FabricName> isPresent = new ArrayList<>();
        List<FabricName> fabricNames = FabricName.getFabrics();
        if(!fabricNames.isEmpty()){
            for(FabricName fabricName : fabricNames){
                String name = fabricName.getName();
                Pattern pattern = Pattern.compile("\\b" + name + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(sentence);
                while(matcher.find()) {
                    isPresent.add(fabricName);
                }
            }
        }
        return isPresent;
    }

    public List<String> getPersons(){
        List<String> persons = null;
        if(StringUtils.isNotBlank(title)){
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("en-ner-person.bin");
            if(inputStream != null){
                try {
                    TokenNameFinderModel nameFinder = new TokenNameFinderModel(inputStream);
                    NameFinderME nameFinderME = new NameFinderME(nameFinder);

                    String[] tokens = getTokens();
                    Span[] spans = nameFinderME.find(tokens);

                    if(spans != null && spans.length > 0){
                        persons = new ArrayList<>();
                        for(int x=0; x < spans.length; x++){
                            String object = tokens[spans[x].getStart()] + " " + tokens[spans[x].getStart() + 1];
                            persons.add(object.trim());
                        }
                    }
                    nameFinderME.clearAdaptiveData();

                } catch (Exception e){
                    logger.debug("Error in getting persons or entities. Message : "+ e.getMessage());
                } finally {
                    try {
                        inputStream.close();
                    } catch (Exception e){
                        logger.debug("Error in closing input stream in get persons. Message : "+ e.getMessage());
                    }
                }
            }
        }
        return persons;
    }

    public List<String> getLocations(){
        List<String> locations = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("en-ner-location.bin");
        if(inputStream != null){
            try{
                TokenNameFinderModel nameFinder = new TokenNameFinderModel(inputStream);
                NameFinderME nameFinderME = new NameFinderME(nameFinder);

                String[] tokens = getTokens();
                Span[] spans = nameFinderME.find(tokens);

                if(spans != null && spans.length > 0){
                    locations = new ArrayList<>();
                    for(int x=0; x < spans.length; x++){
                        String object = tokens[spans[x].getStart()] + " " + tokens[spans[x].getStart() + 1];
                        locations.add(object.trim());
                    }
                }
                nameFinderME.clearAdaptiveData();
            } catch (Exception e){
                logger.debug("Error in get location out of content. Message : "+ e.getMessage());
            } finally {
                try{
                    inputStream.close();
                } catch (Exception e){
                    logger.debug("Error in closing input stream. Message : "+ e.getMessage());
                }
            }
        }
        return locations;
    }

    public List<String> getTimes(){
        List<String> times = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("en-ner-time.bin");
        if(inputStream != null) {
            try {
                TokenNameFinderModel nameFinder = new TokenNameFinderModel(inputStream);
                NameFinderME nameFinderME = new NameFinderME(nameFinder);

                String[] tokens = getTokens();
                Span[] spans = nameFinderME.find(tokens);

                if(spans != null && spans.length > 0){
                    times = new ArrayList<>();
                    for(int x=0; x < spans.length; x++){
                        String object = tokens[spans[x].getStart()] + " " + tokens[spans[x].getStart() + 1];
                        times.add(object.trim());
                    }
                }
                nameFinderME.clearAdaptiveData();
            } catch (Exception e){
                logger.debug("Error in get times out of content. Message : "+ e.getMessage());
            } finally {
                try{
                    inputStream.close();
                } catch (Exception e){
                    logger.debug("Error in closing input stream. Message : "+ e.getMessage());
                }
            }
        }
        return times;
    }

    public List<String> getOrganizations(){
        List<String> organizations = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("en-ner-organization.bin");
        if(inputStream != null) {
            try {
                TokenNameFinderModel nameFinder = new TokenNameFinderModel(inputStream);
                NameFinderME nameFinderME = new NameFinderME(nameFinder);

                String[] tokens = getTokens();
                Span[] spans = nameFinderME.find(tokens);

                if(spans != null && spans.length > 0){
                    organizations = new ArrayList<>();
                    for(int x=0; x < spans.length; x++){
                        String object = tokens[spans[x].getStart()] + " " + tokens[spans[x].getStart() + 1];
                        organizations.add(object.trim());
                    }
                }
                nameFinderME.clearAdaptiveData();
            } catch (Exception e){
                logger.debug("Error in get times out of content. Message : "+ e.getMessage());
            } finally {
                try{
                    inputStream.close();
                } catch (Exception e){
                    logger.debug("Error in closing input stream. Message : "+ e.getMessage());
                }
            }
        }
        return organizations;
    }

    public List<String> getMonies(){
        List<String> monies = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("en-ner-money.bin");
        if(inputStream != null) {
            try {
                TokenNameFinderModel nameFinder = new TokenNameFinderModel(inputStream);
                NameFinderME nameFinderME = new NameFinderME(nameFinder);

                String[] tokens = getTokens();
                Span[] spans = nameFinderME.find(tokens);

                if(spans != null && spans.length > 0){
                    monies = new ArrayList<>();
                    for(int x=0; x < spans.length; x++){
                        String object = tokens[spans[x].getStart()] + " " + tokens[spans[x].getStart() + 1];
                        monies.add(object.trim());
                    }
                }
                nameFinderME.clearAdaptiveData();
            } catch (Exception e){
                logger.debug("Error in get times out of content. Message : "+ e.getMessage());
            } finally {
                try{
                    inputStream.close();
                } catch (Exception e){
                    logger.debug("Error in closing input stream. Message : "+ e.getMessage());
                }
            }
        }
        return monies;
    }

    public List<String> getPercentages(){
        List<String> percentages = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("en-ner-percentage.bin");
        if(inputStream != null) {
            try {
                TokenNameFinderModel nameFinder = new TokenNameFinderModel(inputStream);
                NameFinderME nameFinderME = new NameFinderME(nameFinder);

                String[] tokens = getTokens();
                Span[] spans = nameFinderME.find(tokens);

                if(spans != null && spans.length > 0){
                    percentages = new ArrayList<>();
                    for(int x=0; x < spans.length; x++){
                        String object = tokens[spans[x].getStart()] + " " + tokens[spans[x].getStart() + 1];
                        percentages.add(object.trim());
                    }
                }
                nameFinderME.clearAdaptiveData();
            } catch (Exception e){
                logger.debug("Error in get times out of content. Message : "+ e.getMessage());
            } finally {
                try{
                    inputStream.close();
                } catch (Exception e){
                    logger.debug("Error in closing input stream. Message : "+ e.getMessage());
                }
            }
        }
        return percentages;
    }
}
