package com.gokhanozg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class Main {

    private static final String encoding = "utf-8";
    private static final Locale locale = Locale.forLanguageTag("tr");
    private static String[] ignoredChars = {"#", "'", "\\.", ",", "!", "\\?", ":", ";"};
    private static final String lineSep = System.lineSeparator();
    private static final String[] tweetOwners = {"tayyip", "kilicdar"};

    public static void main(String[] args) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        System.out.println("Preparing score maps...");
        List<String> words = getResourceVals("words");
        List<String> vals = getResourceVals("vals");
        List<String> pvals = getResourceVals("pvals");
        List<String> nvals = getResourceVals("nvals");

        Map<String, Float> scoreMap = new HashMap<>();

        final int len = vals.size();
        for (int i = 0; i < len; i++) {
            String wordsElement = words.get(i);
            String[] cswords = wordsElement.split(",");
            String pScoreStr = pvals.get(i);
            String nScoreStr = nvals.get(i);
            Float score = calculateObjectiveScore(pScoreStr, nScoreStr);
            for (String csword : cswords) {
                String trimmedLowercase = csword.trim().toLowerCase(locale);
                scoreMap.put(trimmedLowercase, score);
            }
        }
        stopWatch.split();
        long elapsed = stopWatch.getSplitTime();
        System.out.println(String.format("Completed creating score map in %s milliseconds", elapsed));

        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

        for (String tweetOwner : tweetOwners) {
            Map<String, Integer> ownerWordCount = new HashMap<>();
            System.out.println(String.format("Building score CSV for %s", tweetOwner));

            List<String> tayyipAllTweets = getResourceVals(tweetOwner);
            tayyipAllTweets = normalizeTweets(tayyipAllTweets);

            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("SENTIMENT_SCORE").append(lineSep);
//    csvBuilder.append("ORIGINAL_TWEET").append(",").append("DETECTED_WORDS").append(",")
//        .append("SENTIMENT_SCORE").append(lineSep);

            for (String tweet : tayyipAllTweets) {
//      csvBuilder.append(tweet).append(",");
                String[] tweetWords = tweet.split(" ");
                Float totalScore = 0f;
                Set<String> uniqueWords = new HashSet<>();

                for (String tweetWord : tweetWords) {
                    tweetWord = removeIgnoredCharacters(tweetWord);
                    tweetWord = tweetWord.trim().toLowerCase(locale);
                    if (StringUtils.isBlank(tweetWord)) {
                        continue;
                    }
                    String[] normalizedTweets = tweetWord.split(" "); // removing chars might split word
                    for (String normalizedTweet : normalizedTweets) {
                        if (StringUtils.isBlank(normalizedTweet) || normalizedTweet.length() == 1) {
                            continue;
                        }
                        if (!ownerWordCount.containsKey(normalizedTweet)) {
                            ownerWordCount.put(normalizedTweet, 1);
                        } else {
                            ownerWordCount.put(normalizedTweet, ownerWordCount.get(normalizedTweet) + 1);
                        }
                        WordAnalysis wordAnalysis = morphology.analyze(normalizedTweet);
                        List<SingleAnalysis> singleAnalyses = wordAnalysis.getAnalysisResults();
                        for (SingleAnalysis singleAnalysis : singleAnalyses) {
                            String root = singleAnalysis.getDictionaryItem().root;
                            if (scoreMap.containsKey(root)) {
                                totalScore += scoreMap.get(root);
                                uniqueWords.add(root);
                            } else if ((!scoreMap.containsKey(root)) && (scoreMap
                                    .containsKey(singleAnalysis.getDictionaryItem().lemma))) {
                                totalScore += scoreMap.get(singleAnalysis.getDictionaryItem().lemma);
                                uniqueWords.add(singleAnalysis.getDictionaryItem().lemma);
                            }
                        }
                    }
                }

                StringBuilder uniqueWordsBuilder = new StringBuilder();
                for (String uniqueWord : uniqueWords) {
                    uniqueWordsBuilder.append(uniqueWord).append("|");
                }
                String uniqueWordsStr = uniqueWords.size() == 0 ? "" : uniqueWordsBuilder.toString();
                uniqueWordsStr =
                        uniqueWordsStr.length() > 0 ? uniqueWordsStr.substring(0, uniqueWordsStr.length() - 1)
                                : "";
                csvBuilder.append(totalScore).append(lineSep);
//      csvBuilder.append(uniqueWordsStr).append(",").append(totalScore).append(lineSep);
            }

            File output = new File(tweetOwner + ".csv");
            String outputStr = csvBuilder.toString();

            FileUtils.writeStringToFile(output, outputStr, "utf-8");
            stopWatch.split();
            elapsed = stopWatch.getSplitTime();
            System.out.println(String.format("Completed creating CSV for %s in %s milliseconds", tweetOwner, elapsed));
            PriorityQueue<Word> wordPriorityQueue = new PriorityQueue<>();

            for (Map.Entry<String, Integer> tweetWordEntry : ownerWordCount.entrySet()) {
                Word tweetWord = new Word(tweetWordEntry.getKey(), tweetWordEntry.getValue());
                wordPriorityQueue.add(tweetWord);
            }

            StringBuilder wordCountBuilder = new StringBuilder();
            while (wordPriorityQueue.size() > 0 && wordPriorityQueue.peek().getCount() >= 50) {
                Word top = wordPriorityQueue.poll();
                wordCountBuilder.append(top.getValue()).append("->").append(top.getCount()).append(lineSep);
            }

            File wordCountFile = new File(tweetOwner + "_words.txt");
            String wordCountOut = wordCountBuilder.toString();
            FileUtils.writeStringToFile(wordCountFile, wordCountOut, encoding);
        }

        stopWatch.stop();
        elapsed = stopWatch.getTime();
        System.out.println(String.format("Completed all in %s milliseconds", elapsed));
    }

    private static List<String> normalizeTweets(List<String> allTweets) {
        List<String> normalizedTweets = new ArrayList<>();
        for (String normalizedTweet : allTweets) {
            if (StringUtils.isBlank(normalizedTweet)) {
                continue;
            }
            if (normalizedTweet.endsWith("]")) {
                continue;
            }
            int firstQuote = normalizedTweet.indexOf("\"");
            normalizedTweet = normalizedTweet.substring(firstQuote + 1);
            normalizedTweet = normalizedTweet.substring(0, normalizedTweet.length() - 1);
            normalizedTweets.add(normalizedTweet);
        }
        return normalizedTweets;
    }

    private static String removeIgnoredCharacters(String tweetWord) {
        for (String ignoredChar : ignoredChars) {
            tweetWord = tweetWord.replaceAll(ignoredChar, " ");
        }
        return tweetWord;
    }

    private static Float calculateObjectiveScore(String pScoreStr, String nScoreStr) {
        float p = 0;
        float n = 0;
        if (StringUtils.isNotBlank(pScoreStr)) {
            p = Float.parseFloat(pScoreStr);
        }
        if (StringUtils.isNotBlank(nScoreStr)) {
            n = Float.parseFloat(nScoreStr);
        }
        if (n > 0) {
            n = -n;
        }
        return p + n;
    }

    private static List<String> getResourceVals(String resource) throws IOException {
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            inputStream = Main.class.getClassLoader().getResourceAsStream("/" + resource);
        }
        if (inputStream == null) {
            throw new IllegalStateException("Couldn't read mandatory resource:" + resource);
        }
        String[] results = IOUtils.toString(inputStream, encoding).split(System.lineSeparator());
        return new ArrayList<>(Arrays.asList(results));
    }
}
