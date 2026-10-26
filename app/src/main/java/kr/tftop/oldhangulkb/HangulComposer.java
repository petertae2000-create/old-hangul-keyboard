package kr.tftop.oldhangulkb;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** 두벌식 모아쓰기 + 옛 한글(첫가끝 조합형) 자동기. 안드로이드 의존 없음. */
public final class HangulComposer {
    static final String VOW = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ";
    static final Map<Character, Integer> CHO = new HashMap<>();
    static final Map<Character, Integer> JUNG = new HashMap<>();
    static final Map<Character, Integer> JONG = new HashMap<>();
    static final Map<String, Character> VC = new HashMap<>();
    static final Map<String, Character> JC = new HashMap<>();
    static final Map<Character, char[]> JS = new HashMap<>();

    static {
        String cho = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
        for (int i = 0; i < cho.length(); i++) CHO.put(cho.charAt(i), 0x1100 + i);
        CHO.put('ㅸ', 0x112B); CHO.put('ㆄ', 0x1157); CHO.put('ㅿ', 0x1140);
        CHO.put('ᄾ', 0x113E); CHO.put('ㆁ', 0x114C); CHO.put('ㆆ', 0x1159); CHO.put('ㅱ', 0x111D);
        for (int i = 0; i < VOW.length(); i++) JUNG.put(VOW.charAt(i), 0x1161 + i);
        JUNG.put('ㆍ', 0x119E); JUNG.put('ㆎ', 0x11A1);
        String jong = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
        for (int i = 0; i < jong.length(); i++) JONG.put(jong.charAt(i), 0x11A8 + i);
        JONG.put('ㅿ', 0x11EB); JONG.put('ㆁ', 0x11F0); JONG.put('ㆆ', 0x11F9);
        JONG.put('ㅸ', 0x11E6); JONG.put('ㆄ', 0x11F4); JONG.put('ㅱ', 0x11E2);
        String[][] vc = {{"ㅗㅏ","ㅘ"},{"ㅗㅐ","ㅙ"},{"ㅗㅣ","ㅚ"},{"ㅜㅓ","ㅝ"},{"ㅜㅔ","ㅞ"},{"ㅜㅣ","ㅟ"},{"ㅡㅣ","ㅢ"},{"ㆍㅣ","ㆎ"}};
        for (String[] p : vc) VC.put(p[0], p[1].charAt(0));
        String[][] jc = {{"ㄱㅅ","ㄳ"},{"ㄴㅈ","ㄵ"},{"ㄴㅎ","ㄶ"},{"ㄹㄱ","ㄺ"},{"ㄹㅁ","ㄻ"},{"ㄹㅂ","ㄼ"},{"ㄹㅅ","ㄽ"},{"ㄹㅌ","ㄾ"},{"ㄹㅍ","ㄿ"},{"ㄹㅎ","ㅀ"},{"ㅂㅅ","ㅄ"}};
        for (String[] p : jc) { JC.put(p[0], p[1].charAt(0)); JS.put(p[1].charAt(0), new char[]{p[0].charAt(0), p[0].charAt(1)}); }
    }

    public static boolean isVowel(char c) { return JUNG.containsKey(c); }

    private Character cho, jung, jong;
    private boolean lastVowel;
    private final Deque<Object[]> hist = new ArrayDeque<>();
    private final StringBuilder out = new StringBuilder();

    static String nfc(String s) { return Normalizer.normalize(s, Normalizer.Form.NFC); }

    static String standalone(char c) { return c == 'ᄾ' ? "ᄾᅠ" : String.valueOf(c); }

    private String syl() {
        if (cho != null && jung != null) {
            StringBuilder b = new StringBuilder();
            b.append((char) (int) CHO.get(cho)).append((char) (int) JUNG.get(jung));
            if (jong != null) b.append((char) (int) JONG.get(jong));
            return b.toString();
        }
        if (cho != null) return standalone(cho);
        if (jung != null) return String.valueOf(jung);
        return "";
    }

    private void save() { hist.push(new Object[]{cho, jung, jong, lastVowel}); }

    public void commit() {
        out.append(nfc(syl()));
        cho = null; jung = null; jong = null; hist.clear(); lastVowel = false;
    }

    public void consonant(char c) {
        save();
        if (cho == null && jung == null) cho = c;
        else if (jung == null || cho == null) { commit(); cho = c; }
        else if (jong == null) { if (JONG.containsKey(c)) jong = c; else { commit(); cho = c; } }
        else { Character k = JC.get("" + jong + c); if (k != null) jong = k; else { commit(); cho = c; } }
        lastVowel = false;
    }

    public void vowel(char v) {
        if (jong != null) {
            char[] sp = JS.get(jong);
            char nc;
            if (sp != null) { jong = sp[0]; nc = sp[1]; } else { nc = jong; jong = null; }
            commit(); save(); cho = nc; jung = v; lastVowel = true; return;
        }
        save();
        if (cho == null && jung == null) jung = v;
        else if (cho != null && jung == null) jung = v;
        else {
            Character k = lastVowel ? VC.get("" + jung + v) : null;
            if (k != null) jung = k; else { commit(); save(); jung = v; }
        }
        lastVowel = true;
    }

    /** @return true면 조합 중인 글자에서 한 조각을 지움, false면 바깥 글을 지워야 함 */
    public boolean backspace() {
        if (!hist.isEmpty()) {
            Object[] h = hist.pop();
            cho = (Character) h[0]; jung = (Character) h[1]; jong = (Character) h[2]; lastVowel = (Boolean) h[3];
            return true;
        }
        if (cho != null || jung != null) { cho = null; jung = null; jong = null; lastVowel = false; return true; }
        return false;
    }

    public boolean isComposing() { return cho != null || jung != null; }
    public String composing() { return nfc(syl()); }
    public String takeCommitted() { String s = out.toString(); out.setLength(0); return s; }
    public void reset() { cho = null; jung = null; jong = null; hist.clear(); lastVowel = false; out.setLength(0); }
}
