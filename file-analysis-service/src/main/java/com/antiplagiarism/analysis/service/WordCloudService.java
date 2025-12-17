package com.antiplagiarism.analysis.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WordCloudService {

    private static final Set<String> STOP_WORDS = Set.of(
            "и", "в", "на", "с", "по", "для", "от", "к", "из", "о", "за", "при", "до",
            "не", "но", "а", "или", "что", "как", "это", "так", "же", "то", "все",
            "она", "он", "они", "мы", "вы", "я", "ты", "его", "её", "их", "ее",
            "был", "была", "было", "были", "быть", "есть", "будет",
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "of", "to", "in", "for", "on", "with", "at", "by", "from", "as", "into",
            "that", "which", "this", "these", "those", "it", "its",
            "and", "or", "but", "if", "then", "else", "when", "where", "who", "what"
    );

    private static final int MIN_WORD_LEN = 3;

    public List<WordFrequency> generateWordCloud(String text, int maxWords) {
        if (text == null || text.isEmpty()) return Collections.emptyList();

        Map<String, Integer> counts = new HashMap<>();
        String[] words = text.toLowerCase().replaceAll("[^a-zа-яё\\s]", " ").split("\\s+");

        for (String word : words) {
            word = word.trim();
            if (word.length() >= MIN_WORD_LEN && !STOP_WORDS.contains(word)) {
                counts.merge(word, 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxWords)
                .map(e -> new WordFrequency(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public WordCloudResult generateWordCloudWithSizes(String text, int maxWords) {
        List<WordFrequency> freqs = generateWordCloud(text, maxWords);
        if (freqs.isEmpty()) return new WordCloudResult(Collections.emptyList(), 0);

        int maxFreq = freqs.get(0).getFrequency();
        int minFreq = freqs.get(freqs.size() - 1).getFrequency();

        List<WordWithSize> sized = freqs.stream()
                .map(wf -> {
                    int size = (maxFreq == minFreq) ? 24 
                            : 12 + (int) ((wf.getFrequency() - minFreq) * 36.0 / (maxFreq - minFreq));
                    return new WordWithSize(wf.getWord(), wf.getFrequency(), size);
                })
                .collect(Collectors.toList());

        int total = freqs.stream().mapToInt(WordFrequency::getFrequency).sum();
        return new WordCloudResult(sized, total);
    }

    public static class WordFrequency {
        private String word;
        private int frequency;

        public WordFrequency(String word, int frequency) {
            this.word = word;
            this.frequency = frequency;
        }

        public String getWord() { return word; }
        public int getFrequency() { return frequency; }
    }

    public static class WordWithSize {
        private String word;
        private int frequency;
        private int fontSize;

        public WordWithSize(String word, int frequency, int fontSize) {
            this.word = word;
            this.frequency = frequency;
            this.fontSize = fontSize;
        }

        public String getWord() { return word; }
        public int getFrequency() { return frequency; }
        public int getFontSize() { return fontSize; }
    }

    public static class WordCloudResult {
        private List<WordWithSize> words;
        private int totalWordsAnalyzed;

        public WordCloudResult(List<WordWithSize> words, int totalWordsAnalyzed) {
            this.words = words;
            this.totalWordsAnalyzed = totalWordsAnalyzed;
        }

        public List<WordWithSize> getWords() { return words; }
        public int getTotalWordsAnalyzed() { return totalWordsAnalyzed; }
    }
}
