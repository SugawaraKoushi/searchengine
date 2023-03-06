package searchengine.dto.indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LemmaFinder {
    private static final LemmaFinder INSTANCE = new LemmaFinder();
    private final LuceneMorphology englishMorphology;
    private final LuceneMorphology russianMorphology;
    private static final String[] PARTICLES_NAMES = new String[]{
            "МЕЖД", "СОЮЗ", "ПРЕДЛ", "ЧАСТ", "PART", "ARTICLE", "PREP", "CONJ", "INT"
    };

    private LemmaFinder() {
        try {
            englishMorphology = new EnglishLuceneMorphology();
            russianMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public synchronized HashMap<String, Integer> getLemmas(String text) {
        HashMap<String, Integer> lemmasMap = new HashMap<>();
        String[] englishWords = getEnglishWordsArray(text);
        String[] russianWords = getRussianWordsArray(text);

        addLemmaToMap(lemmasMap, englishWords, englishMorphology);
        addLemmaToMap(lemmasMap, russianWords, russianMorphology);

        return lemmasMap;
    }

    private synchronized void addLemmaToMap(Map<String, Integer> map, String[] words, LuceneMorphology morphology) {
        for (String word : words) {
            if (word.isBlank())
                continue;

            List<String> wordBaseForms = morphology.getMorphInfo(word);
            if (anyWordBaseBelongsToParticle(wordBaseForms))
                continue;

            List<String> normalForms = morphology.getNormalForms(word);
            if (normalForms.isEmpty())
                continue;

            String normalForm = normalForms.get(0);
            if (map.containsKey(normalForm)) {
                map.put(normalForm, map.get(normalForm) + 1);
            } else {
                map.put(normalForm, 1);
            }
        }
    }

    private String[] getEnglishWordsArray(String text) {
        return text
                .toLowerCase()
                .replaceAll("[^a-z\\s]", " ")
                .trim()
                .split("\\s+");
    }

    private String[] getRussianWordsArray(String text) {
        return text
                .toLowerCase()
                .replaceAll("[^а-я\\s]", " ")
                .trim()
                .split("\\s+");
    }

    private synchronized boolean hasParticleProperty(String wordBase) {
        for (String property : PARTICLES_NAMES) {
            if (wordBase.toUpperCase().contains(property))
                return true;
        }

        return false;
    }

    private synchronized boolean anyWordBaseBelongsToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    public static synchronized LemmaFinder getInstance() {
        return INSTANCE;
    }
}
