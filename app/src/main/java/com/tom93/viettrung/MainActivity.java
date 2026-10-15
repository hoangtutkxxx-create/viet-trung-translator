
package com.tom93.viettrung;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

public class MainActivity extends Activity {
    EditText input;
    TextView chinese, pinyin, status;
    Button translateButton, speakButton, rotateButton;
    TextToSpeech tts;
    String translatedText = "";
    boolean landscape = false;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    int green = Color.rgb(20, 112, 75);

    int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    TextView label(String text, int size) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(Color.rgb(35, 45, 50));
        return v;
    }

    Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(green);
        getWindow().setNavigationBarColor(green);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(247, 249, 247));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = label("DỊCH VIỆT – TRUNG", 25);
        title.setTextColor(green);
        title.setTypeface(null, 1);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView subtitle = label("Tiếng Trung giản thể • Pinyin • Phát âm", 15);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        TextView prompt = label("Nhập câu tiếng Việt:", 18);
        prompt.setTypeface(null, 1);
        prompt.setPadding(0, dp(20), 0, dp(8));
        root.addView(prompt);

        input = new EditText(this);
        input.setHint("Ví dụ: Xin chào, tôi là người Việt Nam");
        input.setTextSize(18);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(3);
        input.setPadding(dp(12), dp(12), dp(12), dp(12));
        input.setBackgroundColor(Color.WHITE);
        root.addView(input, new LinearLayout.LayoutParams(-1, dp(120)));

        translateButton = makeButton("Dịch sang tiếng Trung");
        root.addView(translateButton, new LinearLayout.LayoutParams(-1, dp(54)));
        translateButton.setOnClickListener(v -> translate());

        TextView zhLabel = label("中文 · Tiếng Trung:", 18);
        zhLabel.setTypeface(null, 1);
        zhLabel.setPadding(0, dp(20), 0, dp(8));
        root.addView(zhLabel);

        chinese = label("Bản dịch sẽ hiện ở đây", 30);
        chinese.setTextColor(green);
        chinese.setGravity(Gravity.CENTER);
        chinese.setPadding(dp(10), dp(22), dp(10), dp(22));
        chinese.setBackgroundColor(Color.WHITE);
        root.addView(chinese, new LinearLayout.LayoutParams(-1, -2));

        pinyin = label("Pinyin sẽ hiện ở đây", 19);
        pinyin.setGravity(Gravity.CENTER);
        pinyin.setPadding(0, dp(16), 0, dp(16));
        root.addView(pinyin);

        speakButton = makeButton("🔊 Phát âm tiếng Trung");
        root.addView(speakButton, new LinearLayout.LayoutParams(-1, dp(54)));
        speakButton.setOnClickListener(v -> speak());

        rotateButton = makeButton("⛶ Phóng to màn hình (xoay ngang)");
        root.addView(rotateButton, new LinearLayout.LayoutParams(-1, dp(54)));
        rotateButton.setOnClickListener(v -> rotate());

        status = label("Cần Internet để dịch. Phát âm dùng giọng đọc trên điện thoại.", 13);
        status.setPadding(0, dp(10), 0, dp(5));
        root.addView(status);

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
            }
        });
    }

    void translate() {
        String text = input.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "Anh nhập câu tiếng Việt trước nhé.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(input.getWindowToken(), 0);

        translateButton.setEnabled(false);
        status.setText("Đang dịch...");

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                String address =
                    "https://translate.googleapis.com/translate_a/single"
                    + "?client=gtx&sl=vi&tl=zh-CN&dt=t&q="
                    + URLEncoder.encode(text, "UTF-8");

                connection = (HttpURLConnection) new URL(address).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    throw new Exception("Máy chủ dịch trả về lỗi " + responseCode);
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"));

                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                reader.close();

                JSONArray response = new JSONArray(body.toString());
                JSONArray chunks = response.getJSONArray(0);
                StringBuilder result = new StringBuilder();

                for (int i = 0; i < chunks.length(); i++) {
                    JSONArray chunk = chunks.optJSONArray(i);
                    if (chunk != null && !chunk.isNull(0)) {
                        result.append(chunk.optString(0, ""));
                    }
                }

                String zh = result.toString().trim();
                if (zh.isEmpty() || zh.equalsIgnoreCase(text)) {
                    throw new Exception("Không nhận được bản dịch hợp lệ.");
                }

                String py = toPinyin(zh);

                runOnUiThread(() -> {
                    translatedText = zh;
                    chinese.setText(zh);
                    pinyin.setText("Pinyin: " + py);
                    status.setText("Đã dịch xong.");
                    translateButton.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Dịch thất bại. Kiểm tra Internet rồi thử lại.");
                    translateButton.setEnabled(true);
                    Toast.makeText(this, "Chưa dịch được. Anh thử lại nhé.",
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    String toPinyin(String text) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            try {
                String[] py = PinyinHelper.toHanyuPinyinStringArray(c, format);

                if (py != null && py.length > 0) {
                    out.append(py[0]).append(' ');
                } else {
                    out.append(c);
                }
            } catch (Exception e) {
                out.append(c);
            }
        }

        return out.toString().trim();
    }

    void speak() {
        if (translatedText.isEmpty()) {
            Toast.makeText(this, "Anh dịch câu trước rồi hãy nghe nhé.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (tts != null) {
            int result = tts.setLanguage(Locale.SIMPLIFIED_CHINESE);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this,
                        "Điện thoại chưa có giọng đọc tiếng Trung.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            tts.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, "zh_speech");
        }
    }

    void rotate() {
        landscape = !landscape;

        if (landscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            chinese.setTextSize(44);
            rotateButton.setText("↩ Thu nhỏ màn hình (xoay dọc)");
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            chinese.setTextSize(30);
            rotateButton.setText("⛶ Phóng to màn hình (xoay ngang)");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        executor.shutdownNow();
        super.onDestroy();
    }
}
