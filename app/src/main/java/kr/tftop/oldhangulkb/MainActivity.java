package kr.tftop.oldhangulkb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 설치 안내 화면: 자판 켜기 → 자판 고르기 → 시험 쓰기 */
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Typeface face;
        try { face = Typeface.createFromAsset(getAssets(), "oldhangul.otf"); } catch (Exception e) { face = Typeface.DEFAULT; }
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(p, p * 2, p, p);
        box.setBackgroundColor(Color.parseColor("#FAFAF8"));

        TextView title = text("옛한글 자판", 28, true);
        box.addView(title);
        box.addView(text("TFTOP EDU · 옛 한글로 읽는 영어 소리", 13, false));
        box.addView(text("\n① 아래 단추로 설정을 열어 '옛한글 자판 (TFTOP EDU)'을 켭니다.\n   (안내 창이 뜨면 '확인')", 16, false));
        Button on = new Button(this);
        on.setText("① 자판 켜기 (설정 열기)");
        on.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        box.addView(on);
        box.addView(text("\n② 지금 쓸 자판으로 고릅니다. 카카오톡에서도 입력창 아래 자판 단추로 바꿀 수 있습니다.", 16, false));
        Button pick = new Button(this);
        pick.setText("② 자판 고르기");
        pick.setOnClickListener(v -> ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker());
        box.addView(pick);
        box.addView(text("\n③ 여기서 시험해 보세요. 옛 → ㅂ → ㅐ → ㄴ 을 누르면 ᄫᅢᆫ(van)이 됩니다.", 16, false));
        EditText test = new EditText(this);
        test.setTypeface(face);
        test.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        test.setHint("여기에 쓰기");
        box.addView(test);
        box.addView(text("\n옛 단추 + 글쇠: ㅂ→ㅸ(v) · ㅍ→ㆄ(f, ph) · ㅅ→ㅿ(z) · Shift+ㅅ→ᄾ(sh) · ㅇ→ㆁ(ng) · ㅎ→ㆆ · ㅏ→ㆍ · ㅁ→ㅱ", 15, false));
        TextView warn = text("\n알아 두기: 카카오톡 등 다른 앱은 그 앱의 글꼴로 보여 줍니다. 휴대폰 글꼴이 옛 한글을 모아 그리지 못하면 ᄫᅢᆫ이 '밴'처럼 보일 수 있습니다. 확실히 보여 주려면 화면을 캡처해 그림으로 보내세요.", 14, false);
        warn.setTextColor(Color.parseColor("#B3352B"));
        box.addView(warn);
        box.addView(text("\nAI 작성 초안 → 상담자 검증 필수 · DA", 12, false));

        for (int i = 0; i < box.getChildCount(); i++) if (box.getChildAt(i) instanceof TextView && !(box.getChildAt(i) instanceof Button) && !(box.getChildAt(i) instanceof EditText)) ((TextView) box.getChildAt(i)).setTypeface(face, ((TextView) box.getChildAt(i)).getTypeface() != null && ((TextView) box.getChildAt(i)).getTypeface().isBold() ? Typeface.BOLD : Typeface.NORMAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.parseColor("#1B2130"));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }
}
