package cn.mdict.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxDictBase;

public class WordSuggestion {
    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz"
            .toCharArray();
    private static Map<String, String> langModel;

    public static String getMdxSuggestWord(Context context, MdxDictBase dict,
                                           String inputWord) {
        if (hasDoubleByteChar(inputWord) || inputWord.length() > 50)
            return "";
        DictEntry entry;
        String irregularVerb = null;
        if (langModel == null) {
            try {
                langModel = buildLanguageModel(context, "irregular_verbs.txt");
                irregularVerb = langModel.get(inputWord);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (irregularVerb != null) {
            return irregularVerb;
        }
        String input = inputWord.trim().toLowerCase();
        if (dict != null && dict.isValid()) {
            if (input.endsWith("er")) {
                String word = input.substring(0, input.length() - 2);

                entry = new DictEntry();
                entry.setHeadword("");
                if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    if (compareWord(entry.getHeadword(), word, 0))
                        return word;
                }

                if (word.endsWith("i")) {
                    word = word.substring(0, word.length() - 1) + "y";
                    entry = new DictEntry();
                    entry.setHeadword("");
                    if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                        if (compareWord(entry.getHeadword(), word, 0))
                            return word;
                    }
                }

                // 重读闭音节，双写最后一个辅音字母
                if (input.length() > 5) {
                    if (input.substring(input.length() - 3, input.length() - 3)
                            .equals(input.substring(input.length() - 4,
                                    input.length() - 4))) {
                        word = input.substring(0, input.length() - 3);
                        entry = new DictEntry();
                        entry.setHeadword("");
                        if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                            if (compareWord(entry.getHeadword(), word, 0))
                                return word;
                        }
                    }
                }
            }
            if (input.endsWith("est")) {
                String word = input.substring(0, input.length() - 3);
                entry = new DictEntry();
                entry.setHeadword("");
                if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    if (compareWord(entry.getHeadword(), word, 0))
                        return word;
                }

                if (word.endsWith("i")) {
                    word = word.substring(0, word.length() - 1) + "y";
                    entry = new DictEntry();
                    entry.setHeadword("");
                    if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                        if (compareWord(entry.getHeadword(), word, 0))
                            return word;
                    }
                }

                // 重读闭音节，双写最后一个辅音字母
                if (input.length() > 6) {
                    if (input.substring(input.length() - 4, input.length() - 4)
                            .equals(input.substring(input.length() - 5,
                                    input.length() - 5))) {
                        word = input.substring(0, input.length() - 4);
                        entry = new DictEntry();
                        entry.setHeadword("");
                        if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                            if (compareWord(entry.getHeadword(), word, 0))
                                return word;
                        }
                    }
                }
            }
            if (input.endsWith("s")) {
                String word = input.substring(0, input.length() - 1);
                entry = new DictEntry();
                entry.setHeadword("");
                if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    if (compareWord(entry.getHeadword(), word, 0))
                        return word;
                }

                if (input.endsWith("es")) {
                    word = input.substring(0, input.length() - 2);

                    entry = new DictEntry();
                    entry.setHeadword("");
                    if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                        if (compareWord(entry.getHeadword(), word, 0))
                            return word;
                    }

                    //辅音字母 + y结尾的变 y为 i加es
                    if (word.endsWith("i")) {
                        word = word.substring(0, word.length() - 1) + "y";
                        entry = new DictEntry();
                        entry.setHeadword("");
                        if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                            if (compareWord(entry.getHeadword(), word, 0))
                                return word;
                        }
                    }

                    //以f， fe 结尾的 变f或fe为v +es
                    if (word.endsWith("v")) {
                        word = word.substring(0, word.length() - 1) + "fe";
                        entry = new DictEntry();
                        entry.setHeadword("");
                        if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                            if (compareWord(entry.getHeadword(), word, 0))
                                return word;
                        }

                        word = word.substring(0, word.length() - 1);
                        entry = new DictEntry();
                        entry.setHeadword("");
                        if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                            if (compareWord(entry.getHeadword(), word, 0))
                                return word;
                        }
                    }

                }
            }
            if (input.endsWith("ed")) {
                // Normal Case
                String word = input.substring(0, input.length() - 2);
                entry = new DictEntry();
                entry.setHeadword("");
                if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    if (compareWord(entry.getHeadword(), word, 0))
                        return word;
                }

                // 以辅音字母 + y结尾的动词，把-y变为-i 再加-ed
                if (word.endsWith("i")) {
                    word = word.substring(0, word.length() - 1) + "y";
                    entry = new DictEntry();
                    entry.setHeadword("");
                    if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                        if (compareWord(entry.getHeadword(), word, 0))
                            return word;
                    }
                }

                // 以不发音的 -e 结尾动词，动词词尾加 -d
                word = input.substring(0, input.length() - 1); //
                entry = new DictEntry();
                entry.setHeadword("");
                if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    if (compareWord(entry.getHeadword(), word, 0))
                        return word;
                }

                // 重读闭音节，双写最后一个辅音字母
                if (input.length() > 5) {
                    if (input.substring(input.length() - 3, input.length() - 3)
                            .equals(input.substring(input.length() - 4,
                                    input.length() - 4))) {
                        word = input.substring(0, input.length() - 3);
                        entry = new DictEntry();
                        entry.setHeadword("");
                        if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                            if (compareWord(entry.getHeadword(), word, 0))
                                return word;
                        }
                    }
                }
            }

            if (input.endsWith("ing")) {
                // Normal case
                String word = input.substring(0, input.length() - 3);
                entry = new DictEntry();
                entry.setHeadword("");
                if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    if (compareWord(entry.getHeadword(), word, 0))
                        return word;
                }

                // 以不发音的e 结尾的去e 加-ing
                word = input.substring(0, input.length() - 3) + "e"; //
                entry = new DictEntry();
                entry.setHeadword("");
                if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    if (compareWord(entry.getHeadword(), word, 0))
                        return word;
                }

                // 重读闭音节，双写最后一个辅音字母
                if (input.length() > 6) {
                    if (input.substring(input.length() - 4, input.length() - 4)
                            .equals(input.substring(input.length() - 5,
                                    input.length() - 5))) {
                        word = input.substring(0, input.length() - 4);
                        entry = new DictEntry();
                        entry.setHeadword("");
                        if (dict.locateFirst(word, false, false, false, entry) == MdxDictBase.kMdxSuccess) {
                            if (compareWord(entry.getHeadword(), word, 0))
                                return word;
                        }
                    }
                }
            }
            langModel = null;
            entry = null;
        }
        return "";
    }

    public static boolean compareWord(String word1, String word2, int letterTol) {
        String cw1 = word1.trim().toLowerCase();
        String cw2 = word2.trim().toLowerCase();
        if (cw1.equals(cw2))
            return true;
        if (letterTol > 0)
            if (cw1.equals(cw2.substring(0, cw2.length() - letterTol)))
                return true;

        return false;

    }

    public static String getMdxSuggestWordList(Context context,
                                               MdxDictBase dict, String inputWord) {
        if (hasDoubleByteChar(inputWord) || inputWord.length() > 50)
            return "";
        if (dict != null && dict.isValid()) {

            Set<String> dictionary = new HashSet<String>();
            DictEntry entry;

            // 3 wordsInEditDistance代表的是编辑距离
            // buildEditDistance1Set是编辑距离为1的字符的集合
            // buildEditDistance2Set是编辑距离为2的字符的集合
            Set<String> wordsInEditDistance = buildEditDistance1Set(inputWord);
            for (String editDistance : wordsInEditDistance) {
                entry = new DictEntry(DictEntry.kInvalidEntryNo, "", dict
                        .getDictPref().getDictId());
                if (dict.locateFirst(editDistance, true, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    dictionary.add(entry.getHeadword());
                }
            }

			/*
             * if (dictionary.isEmpty()) { wordsInEditDistance =
			 * buildEditDistance2Set(input); for (String editDistance :
			 * wordsInEditDistance) { entry = new
			 * DictEntry(DictEntry.kInvalidEntryNo, "", dict
			 * .getDictPref().getDictId()); if (dict.locateFirst(editDistance,
			 * true, false, entry) == MdxDictBase.kMdxSuccess) {
			 * dictionary.add(entry.getHeadword()); } } if
			 * (dictionary.isEmpty()) { return ""; // Not found } }
			 */
            List<String> guessWords = guessCorrectWord(inputWord, dictionary);

            String guessHtml = "";
            for (String guessWord : guessWords) {
                guessHtml += "<p><a href=\"content://mdict.cn/headword/"
                        + guessWord + "\">" + guessWord + "</a>";
            }
            return guessHtml;
        }

        return "";
    }

    /**
     * 建立编辑距离为1的字符集合，返回类型为Set<String>
     *
     * @param input
     * @return
     * @paramlangModel
     */
    public static Set<String> buildEditDistance1Set(String input) {
        Set<String> wordsInEditDistance = new HashSet<String>();
        char[] characters = input.toCharArray();

        if (input.endsWith("es") || input.endsWith("ed"))
            wordsInEditDistance.add(input.substring(0, input.length() - 2));

        if (input.endsWith("ing"))
            wordsInEditDistance.add(input.substring(0, input.length() - 3));
        // Deletion: delete letter[i]
        // 删除一个字符
        for (int i = 0; i < input.length(); i++)
            wordsInEditDistance.add(input.substring(0, i)
                    + input.substring(i + 1));

        // Transposition: swap letter[i] and letter[i+1]
        // 交换相邻的两个字符
        for (int i = 0; i < input.length() - 1; i++)
            wordsInEditDistance.add(input.substring(0, i) + characters[i + 1]
                    + characters[i] + input.substring(i + 2));

        // Alteration: change letter[i] to a-z
        // 用其他字符代替一个字符
        for (int i = 0; i < input.length(); i++)
            for (char c : ALPHABET)
                wordsInEditDistance.add(input.substring(0, i) + c
                        + input.substring(i + 1));

        // Insertion:insert new letter a-z
        // 插入一个新的字符
        for (int i = 0; i < input.length() + 1; i++)
            for (char c : ALPHABET)
                wordsInEditDistance.add(input.substring(0, i) + c
                        + input.substring(i));

        return wordsInEditDistance;
    }

    /**
     * 建立编辑距离为2的字符集合，返回类型为Set<String>
     * <p/>
     * 在编辑距离为1的字符集合的基础上再次进行建立编辑距离为1的字符集合的操作
     *
     * @param input
     * @return
     * @paramlangModel
     */
    public static Set<String> buildEditDistance2Set(String input) {
        Set<String> wordsInEditDistance1 = buildEditDistance1Set(input);
        Set<String> wordsInEditDistance2 = new HashSet<String>();
        for (String editDistance1 : wordsInEditDistance1)
            wordsInEditDistance2.addAll(buildEditDistance1Set(editDistance1));
        wordsInEditDistance2.addAll(wordsInEditDistance1);
        return wordsInEditDistance2;
    }

    /**
     * 返回猜测的可能符合要求的单词
     *
     * @return
     * @paramlangModel
     * @paramwordsInEditDistance
     */
    public static List<String> guessCorrectWord(final String input,
                                                Set<String> wordsInEditDistance) {
        List<String> words = new LinkedList<String>(wordsInEditDistance);
        // 将满足要求的单词进行排序显示输出
        Collections.sort(words, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return Double.valueOf(((String) o1).length()).compareTo((double) ((String) o2).length());
            }
        });
        // 若符合条件的集合中元素个数大于5个，则仅输出概率最高的前五个单词
        return words.size() > 10 ? words.subList(0, 10) : words;
    }

    private static Map<String, String> buildLanguageModel(Context context,
                                                          String file) throws IOException {
        Map<String, String> langModel = new HashMap<String, String>();

        InputStream inputStream = context.getAssets().open(file);

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream, "utf-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] words = line.split(",");
            String orgWord = words[0].toLowerCase();
            String pastTenseWord = words[1].toLowerCase();
            String pastParticipleWord = words[2].toLowerCase();
            if (pastTenseWord.contains("/")) {
                String[] pastTenseWords = pastTenseWord.split("/");
                langModel.put(pastTenseWords[0], orgWord);
                langModel.put(pastTenseWords[1], orgWord);
            }

            if (pastParticipleWord.contains("/")) {
                String[] pastParticipleWords = pastParticipleWord.split("/");
                langModel.put(pastParticipleWords[0], orgWord);
                langModel.put(pastParticipleWords[1], orgWord);
            }
        }
        return langModel;
    }

    public static boolean isChinese(final String str) {
        Pattern pattern = Pattern.compile("[\\u3400-\\u9FBF]+");// 是否中文表达式
        return str != null && pattern.matcher(str.trim()).find();

    }

    public static boolean hasDoubleByteChar(final String s) {
        for (int i = 0; i < s.length(); i++) {
            int c = String.valueOf(s.charAt(i)).getBytes().length;
            if (c == 2) {
                return true;
            }
        }
        return false;
    }

}
