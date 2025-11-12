package cse.ssuroom.database;

import static org.junit.Assert.fail;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Firestore 쓰기 작업 자체의 성공 여부만 확인하기 위한 아주 단순한 테스트입니다.
 */
@RunWith(AndroidJUnit4.class)
public class ShortTermRepositoryInstrumentedTest {

    private static final String TEST_TAG = "SSUROOM_WRITE_TEST"; // 새로운 로그 태그
    private ShortTermRepository repository;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FirebaseApp.initializeApp(context);
        repository = new ShortTermRepository();
    }

    @Test
    public void simpleWriteTest() throws InterruptedException {
        Log.d(TEST_TAG, "Simple write test started...");

        ShortTerm testShortTerm = createDummyProperty();
        Log.d(TEST_TAG, "1. Dummy object created.");

        repository.save(testShortTerm, propertyId -> {
            if (propertyId != null) {
                Log.d(TEST_TAG, "2. SUCCESS! Document created with ID: " + propertyId);
                // 테스트 데이터 정리를 위해 생성된 ID를 로그로 남김
                Log.d(TEST_TAG, "MANUAL_CLEANUP_ID:" + propertyId);
            } else {
                Log.e(TEST_TAG, "2. FAILED! The save operation returned a null ID.");
                fail("Save operation failed. Check logs for more details.");
            }
        });

        Log.d(TEST_TAG, "3. save() method has been called. Waiting for 5 seconds to see the result in Logcat...");
        Thread.sleep(5000); // 5초 동안 콜백이 오는지 기다림
        Log.d(TEST_TAG, "4. Test finished.");
    }

    private ShortTerm createDummyProperty() {
        return new ShortTerm("DUMMY DATA", "This is a simple test.", "dummy-host", 
                new ArrayList<>(), "DUMMY_ROOM", 0, 0.0, new Date(), new Date(), 
                new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }
}
