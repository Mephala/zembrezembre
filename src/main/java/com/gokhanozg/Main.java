package com.gokhanozg;

import org.apache.commons.io.FileUtils;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.tokenization.TurkishSentenceExtractor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
//        TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;
//        String input = "Merhaba! Bugün 2. köprü Fsm.'de trafik vardı.değil mi?";
//        List<String> sentences = extractor.fromParagraph(input);
//        for (String sentence : sentences) {
//            System.out.println(sentence);
//        }
//
//        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
//        WordAnalysis results = morphology.analyze("Atatürk");
//        results.forEach(s -> System.out.println(s.formatLong()));

        File input = new File("input.txt");
        File output = new File("output.txt");
        String s = "jh";
        FileUtils.writeStringToFile(output,s,"utf-8");
    }
}
