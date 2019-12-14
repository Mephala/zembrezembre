package com.gokhanozg;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import zemberek.core.io.Words;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.tokenization.TurkishSentenceExtractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class Main {
    private static final String encoding = "utf-8";

    public static void main(String[] args) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        System.out.println("Preparing score maps...");
        List<String> words = getResourceVals("words");
        List<String> vals = getResourceVals("vals");

        Map<String, Float> scoreMap = new HashMap<>();

        final int len = vals.size();
        for (int i = 0; i < len; i++) {
            String wordsElement = words.get(i);
            String[] cswords = wordsElement.split(",");
            String scoreStr = vals.get(i);
            Float score = StringUtils.isEmpty(scoreStr) ? 0f : Float.parseFloat(scoreStr);
            for (String csword : cswords) {
                scoreMap.put(csword.trim(), score);
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
            WordAnalysis wordAnalysis = morphology.analyze(tweetWord);
            List<SingleAnalysis> singleAnalyses = wordAnalysis.getAnalysisResults();
            for (SingleAnalysis singleAnalysis : singleAnalyses) {
                String root = singleAnalysis.getDictionaryItem().root;
                if (scoreMap.containsKey(root)) {
                    totalScore += scoreMap.get(root);
                    uniqueWords.add(root);
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
