package searchengine.services.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.morphology.LuceneMorphology;

public record Morphology(LuceneMorphology luceneMorphology) {

    private static final String WORD_FORMS_EXCLUDE_REGEX = ".*\\s(СОЮЗ|МЕЖД|ПРЕДЛ|ЧАСТ)\\s?.*";
    private static final int SNIPPET_SIZE = 230;
    private static final int MAX_SIZE_SENTENCES = 150;
    private static final int MIN_SIZE_SENTENCES = 40;

    public HashMap<String, Integer> collectLemmas(String text) {
        String[] words = getCleanText(text).split("\\s+");
        HashMap<String, Integer> lemmasCount = new HashMap<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            String wordCheck = wordBaseForms.getFirst();
            if (wordCheck.matches(WORD_FORMS_EXCLUDE_REGEX)) {
                continue;
            }
            List<String> wordNormalForms = luceneMorphology.getNormalForms(word);
            if (wordNormalForms.isEmpty()) {
                continue;
            }
            String lemma = wordNormalForms.getFirst();

            if (lemmasCount.containsKey(lemma)) {
                lemmasCount.put(lemma, lemmasCount.get(lemma) + 1);
            } else {
                lemmasCount.put(lemma, 1);
            }
        }
        return lemmasCount;
    }

    private String getCleanText(String text) {
        return text.toLowerCase()
            .replaceAll("[^а-яё\\s]", "")
            .trim();
    }

    public String createSnippet(String content, List<String> queryLemmas) {
        List<String> sentences = splitIntoSentences(content);
        List<String> sentencesWithLemmas = findSentencesWithLemmas(sentences, queryLemmas);

        StringBuilder snippet = new StringBuilder();
        for (String sentence : sentencesWithLemmas) {
            if (sentences.contains("</b> <b>")) {
                sentence = sentence.replaceAll("</b> <b>", " ");
            }
            snippet.append(sentence).append(". ");
            if (snippet.length() > SNIPPET_SIZE) {
                snippet = new StringBuilder(snippet.substring(0, SNIPPET_SIZE) + "...");
                return snippet.toString();
            }
        }
        return snippet.toString();
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentencesList = new ArrayList<>();
        List<String> minSentencesList = new ArrayList<>();
        String[] sentences = text
            .replaceAll("<.*?>", "")
            .split("\\n");
        for (String sentence : sentences) {
            if (sentence.isBlank() || !sentence.matches(".*[А-Яа-я].*")) {
                continue;
            }
            sentence = sentence.replaceAll("[^А-Яа-я\\s.,!?]", "").trim();
            if (sentencesList.contains(sentence) || minSentencesList.contains(sentence)) {
                continue;
            }
            if (sentence.length() >= MAX_SIZE_SENTENCES) {
                String[] subSentences = sentence.split("[.!?] ");
                sentencesList.addAll(Arrays.stream(subSentences).toList());
            } else if (sentence.length() <= MIN_SIZE_SENTENCES) {
                minSentencesList.add(sentence);
            } else {
                sentencesList.add(sentence);
            }
        }
        sentencesList.addAll(minSentencesList);
        return sentencesList;
    }

    private List<String> findSentencesWithLemmas(List<String> sentences, List<String> queryLemmas) {
        List<String> sentencesLemmas = new ArrayList<>();
        for (String sentence : sentences) {
            String[] words = sentence.split(" ");
            StringBuilder newSentences = new StringBuilder();
            boolean hasLemmas = false;
            for (String word : words) {
                String checkWord = word.replaceAll("[^А-яа-я]", "");
                if (checkWord.isEmpty()) {
                    continue;
                }
                List<String> wordBaseForms = luceneMorphology.getNormalForms(
                    checkWord.toLowerCase());
                if (wordBaseForms.isEmpty()) {
                    continue;
                }
                for (String lemma : queryLemmas) {
                    if (wordBaseForms.getFirst().equals(lemma)) {
                        word = word.replaceAll(checkWord, "<b>" + checkWord + "</b>");
                        hasLemmas = true;
                    }
                }
                newSentences.append(" ").append(word);
            }
            if (hasLemmas) {
                sentencesLemmas.add(newSentences.toString().trim());
            }
        }
        return sentencesLemmas;
    }
}
