package cse.ssuroom;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cse.ssuroom.R;

/**
 * 다음 주소 검색 전용 액티비티
 * 전체 화면으로 주소 검색 UI를 표시합니다.
 */
public class DaumAddressActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_ADDRESS = "selected_address";
    public static final int REQUEST_ADDRESS = 1001;

    private WebView webView;
    private boolean isAddressSelected = false; // 중복 호출 방지

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        setupWebView();
        loadDaumAddressPage();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setDisplayZoomControls(false);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.addJavascriptInterface(new AddressJavaScriptInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                android.util.Log.d("DaumAddress", "페이지 로드 완료");
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                android.util.Log.e("DaumAddress", "에러: " + description);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("DaumAddress", "Console: " + consoleMessage.message());
                return true;
            }
        });
    }

    private void loadDaumAddressPage() {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
                "    <title>주소 검색</title>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        html, body { height: 100%; width: 100%; overflow: hidden; }\n" +
                "        #container { width: 100%; height: 100%; }\n" +
                "    </style>\n" +
                "    <script src='https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id='container'></div>\n" +
                "    <script>\n" +
                "        window.onload = function() {\n" +
                "            new daum.Postcode({\n" +
                "                oncomplete: function(data) {\n" +
                "                    var addr = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;\n" +
                "                    console.log('선택된 주소:', addr);\n" +
                "                    \n" +
                "                    if (window.Android) {\n" +
                "                        Android.onAddressSelected(addr);\n" +
                "                    }\n" +
                "                },\n" +
                "                onclose: function() {\n" +
                "                    console.log('주소 검색 창 닫힘');\n" +
                "                    if (window.Android) {\n" +
                "                        Android.onClose();\n" +
                "                    }\n" +
                "                },\n" +
                "                width: '100%',\n" +
                "                height: '100%'\n" +
                "            }).embed(document.getElementById('container'));\n" +
                "        };\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        webView.loadDataWithBaseURL("https://example.com", html, "text/html", "UTF-8", null);
    }

    private class AddressJavaScriptInterface {
        @JavascriptInterface
        public void onAddressSelected(String address) {
            android.util.Log.d("DaumAddress", "onAddressSelected 호출됨: " + address);

            if (isAddressSelected) {
                android.util.Log.d("DaumAddress", "이미 처리됨 - 무시");
                return;
            }

            isAddressSelected = true;
            android.util.Log.d("DaumAddress", "주소 선택 처리 시작");

            runOnUiThread(() -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SELECTED_ADDRESS, address);
                setResult(RESULT_OK, resultIntent);
                android.util.Log.d("DaumAddress", "액티비티 종료");
                finish();
            });
        }

        @JavascriptInterface
        public void onClose() {
            android.util.Log.d("DaumAddress", "onClose 호출됨");

            if (isAddressSelected) {
                android.util.Log.d("DaumAddress", "이미 주소 선택됨 - onClose 무시");
                return;
            }

            android.util.Log.d("DaumAddress", "취소 처리");

            runOnUiThread(() -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}