package com.gokhanozg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    File input = new File("input.txt");
    String tweet = FileUtils.readFileToString(input, encoding);

    String[] tweetWords = tweet.split(" ");
    Float totalScore = 0f;
    Set<String> uniqueWords = new HashSet<>();

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

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

    StringBuilder stringBuilder = new StringBuilder();
    for (String uniqueWord : uniqueWords) {
      stringBuilder.append(uniqueWord).append(System.lineSeparator());
    }
    stringBuilder.append(totalScore).append(System.lineSeparator());

    String outputStr = stringBuilder.toString();

    File output = new File("output.txt");

    FileUtils.writeStringToFile(output, outputStr, "utf-8");
    stopWatch.stop();
    elapsed = stopWatch.getTime();
    System.out.println(String.format("Completed all in %s milliseconds", elapsed));
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
    }//
    String[] results = IOUtils.toString(inputStream, encoding).split(System.lineSeparator());
    return new ArrayList<>(Arrays.asList(results));
  }
}
