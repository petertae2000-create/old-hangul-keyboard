package kr.tftop.oldhangulkb;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/** 옛한글 자판: 두벌식 + '옛' 단추로 ㅸ ㆄ ᄾ ㅿ ㆁ ㆆ ㆍ ㅱ 입력 */
public class OldHangulIME extends InputMethodService {
    private static final String[][] ROWS = {
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"SHIFT","z","x","c","v","b","n","m","BACK"},
            {"OLD","MODE","SPACE","PUNCT","ENTER"}
    };
    private static final Map<String, Character> KEYS = new HashMap<>();
    private static final Map<String, Character> SHIFT = new HashMap<>();
    private static final Map<String, Character> OLD = new HashMap<>();
    private static final Map<String, Character> OLDSHIFT = new HashMap<>();
    static {
        String k = "qㅂwㅈeㄷrㄱtㅅyㅛuㅕiㅑoㅐpㅔaㅁsㄴdㅇfㄹgㅎhㅗjㅓkㅏlㅣzㅋxㅌcㅊvㅍbㅠnㅜmㅡ";
        for (int i = 0; i < k.length(); i += 2) KEYS.put(String.valueOf(k.charAt(i)), k.charAt(i + 1));
        String s = "qㅃwㅉeㄸrㄲtㅆoㅒpㅖ";
        for (int i = 0; i < s.length(); i += 2) SHIFT.put(String.valueOf(s.charAt(i)), s.charAt(i + 1));
        String o = "qㅸvㆄdㆁgㆆkㆍaㅱtㅿ";
        for (int i = 0; i < o.length(); i += 2) OLD.put(String.valueOf(o.charAt(i)), o.charAt(i + 1));
        OLDSHIFT.put("t", 'ᄾ');
    }

    private static final int C_BG = Color.parseColor("#E9EBF0");
    private static final int C_KEY = Color.WHITE;
    private static final int C_KEY_DOWN = Color.parseColor("#C9D3E8");
    private static final int C_MOD = Color.parseColor("#D3D7E0");
    private static final int C_INK = Color.parseColor("#1B2130");
    private static final int C_OLD = Color.parseColor("#B3352B");
    private static final int C_OLD_BG = Color.parseColor("#F8E6E3");
    private static final int C_ACCENT = Color.parseColor("#2F4F8F");

    private final HangulComposer comp = new HangulComposer();
    private final Map<String, TextView> views = new HashMap<>();
    private boolean shift, old, hangul = true;
    private int punct = 0;
    private Typeface face;
    private final Handler repeat = new Handler(Looper.getMainLooper());

    @Override public View onCreateInputView() {
        try { face = Typeface.createFromAsset(getAssets(), "oldhangul.otf"); } catch (Exception e) { face = Typeface.DEFAULT; }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(C_BG);
        int pad = dp(4);
        root.setPadding(pad, pad, pad, pad + dp(6));
        views.clear();
        for (int r = 0; r < ROWS.length; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
            row.setLayoutParams(rp);
            if (r == 1) row.addView(spacer(0.5f));
            for (String code : ROWS[r]) row.addView(makeKey(code));
            if (r == 1) row.addView(spacer(0.5f));
            root.addView(row);
        }
        refresh();
        return root;
    }

    private View spacer(float w) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, w));
        return v;
    }

    private float weight(String code) {
        switch (code) {
            case "SHIFT": case "BACK": return 1.5f;
            case "SPACE": return 4f;
            case "OLD": case "MODE": case "PUNCT": case "ENTER": return 1.4f;
            default: return 1f;
        }
    }

    private TextView makeKey(final String code) {
        final TextView t = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight(code));
        int m = dp(2);
        lp.setMargins(m, m, m, m);
        t.setLayoutParams(lp);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(face);
        t.setTextColor(C_INK);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, code.length() == 1 ? 22 : 14);
        t.setIncludeFontPadding(false);
        t.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                v.setPressed(true);
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                onKey(code);
                if (code.equals("BACK")) repeat.postDelayed(new Runnable() {
                    @Override public void run() { onKey("BACK"); repeat.postDelayed(this, 60); }
                }, 400);
            } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setPressed(false);
                repeat.removeCallbacksAndMessages(null);
            }
            return true;
        });
        views.put(code, t);
        return t;
    }

    private GradientDrawable shape(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(7));
        return g;
    }

    private void paint(TextView t, int normal) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, shape(C_KEY_DOWN));
        s.addState(new int[]{}, shape(normal));
        t.setBackground(s);
    }

    private void refresh() {
        for (Map.Entry<String, TextView> e : views.entrySet()) {
            String code = e.getKey();
            TextView t = e.getValue();
            t.setTextColor(C_INK);
            if (code.length() == 1) {
                SpannableStringBuilder b = new SpannableStringBuilder();
                if (!hangul) {
                    b.append(shift ? code.toUpperCase() : code);
                    paint(t, C_KEY);
                } else {
                    Character main;
                    if (old && OLD.containsKey(code)) main = (shift && OLDSHIFT.containsKey(code)) ? OLDSHIFT.get(code) : OLD.get(code);
                    else if (shift && SHIFT.containsKey(code)) main = SHIFT.get(code);
                    else main = KEYS.get(code);
                    int st = b.length();
                    b.append(HangulComposer.standalone(main));
                    if (old && OLD.containsKey(code)) b.setSpan(new ForegroundColorSpan(C_OLD), st, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (!old && OLD.containsKey(code)) {
                        int s2 = b.length();
                        b.append(code.equals("t") ? "ㅿᄾ" : String.valueOf(OLD.get(code)));
                        b.setSpan(new ForegroundColorSpan(C_OLD), s2, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        b.setSpan(new RelativeSizeSpan(0.5f), s2, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        b.setSpan(new SuperscriptSpan(), s2, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    paint(t, OLD.containsKey(code) ? C_OLD_BG : C_KEY);
                }
                t.setText(b);
            } else {
                String label;
                int bg = C_MOD;
                switch (code) {
                    case "SHIFT": label = "Shift"; if (shift) { bg = C_ACCENT; t.setTextColor(Color.WHITE); } break;
                    case "BACK": label = "지움"; break;
                    case "OLD": label = "옛"; if (old) { bg = C_OLD; t.setTextColor(Color.WHITE); } else t.setTextColor(C_OLD); break;
                    case "MODE": label = hangul ? "한→영" : "영→한"; break;
                    case "SPACE": label = "띄어쓰기"; break;
                    case "PUNCT": label = ".,?!"; break;
                    default: label = "↵"; break;
                }
                t.setText(label);
                paint(t, bg);
            }
        }
    }

    private void onKey(String code) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        switch (code) {
            case "SHIFT": shift = !shift; refresh(); return;
            case "OLD": old = !old; refresh(); return;
            case "MODE": flushAll(ic, ""); hangul = !hangul; shift = false; old = false; refresh(); return;
            case "SPACE": flushAll(ic, " "); return;
            case "PUNCT": flushAll(ic, String.valueOf(".,?!".charAt(punct++ % 4))); return;
            case "ENTER":
                flushAll(ic, "");
                EditorInfo ei = getCurrentInputEditorInfo();
                boolean multi = ei != null && (ei.inputType & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
                if (multi || !sendDefaultEditorAction(true)) ic.commitText("\n", 1);
                return;
            case "BACK":
                if (comp.backspace()) { ic.setComposingText(comp.composing(), 1); if (!comp.isComposing()) ic.finishComposingText(); }
                else { ic.finishComposingText(); sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL); }
                return;
        }
        if (!hangul) {
            ic.commitText(shift ? code.toUpperCase() : code, 1);
            if (shift) { shift = false; refresh(); }
            return;
        }
        Character ch;
        if (old && OLD.containsKey(code)) ch = (shift && OLDSHIFT.containsKey(code)) ? OLDSHIFT.get(code) : OLD.get(code);
        else if (shift && SHIFT.containsKey(code)) ch = SHIFT.get(code);
        else ch = KEYS.get(code);
        if (ch == null) return;
        if (HangulComposer.isVowel(ch)) comp.vowel(ch); else comp.consonant(ch);
        String done = comp.takeCommitted();
        if (!done.isEmpty()) ic.commitText(done, 1);
        ic.setComposingText(comp.composing(), 1);
        if (shift || old) { shift = false; old = false; refresh(); }
    }

    private void flushAll(InputConnection ic, String tail) {
        comp.commit();
        String done = comp.takeCommitted() + tail;
        if (!done.isEmpty()) ic.commitText(done, 1); else ic.finishComposingText();
    }

    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        comp.reset();
    }

    @Override public void onFinishInput() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && comp.isComposing()) ic.finishComposingText();
        comp.reset();
        super.onFinishInput();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
