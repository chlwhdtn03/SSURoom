package cse.ssuroom.database;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PropertyRepository<T extends Property> {

    protected FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String collectionName;
    private final Class<T> propertyClass;
    private final String TAG;

    /**
     * @param collectionName Firestore에서 사용할 컬렉션의 이름
     * @param propertyClass 데이터를 변환할 클래스 타입
     * @param tag Logcat에서 사용할 태그
     */
    public PropertyRepository(String collectionName, Class<T> propertyClass, String tag) {
        this.collectionName = collectionName;
        this.propertyClass = propertyClass;
        this.TAG = tag;
    }

    //  매물 등록 (ID 자동 생성)
    public void save(T property, OnPropertyIdGenerated listener) {
        db.collection(collectionName)
                .add(property)
                .addOnSuccessListener(docRef -> listener.onGenerated(docRef.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving document to " + collectionName, e);
                    listener.onGenerated(null);
                });
    }
    //  특정 매물 조회
    public void findById(String propertyId, OnPropertyLoaded<T> listener) {
        db.collection(collectionName)
                .document(propertyId)
                .get()
                .addOnSuccessListener(docSnap -> {
                    if (docSnap.exists()) {
                        T property = docSnap.toObject(propertyClass);
                        //  문서 ID 설정
                        if (property != null) {
                            property.setPropertyId(docSnap.getId());
                        }
                        listener.onLoaded(property);
                    } else {
                        listener.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding document in " + collectionName, e);
                    listener.onLoaded(null);
                });
    }

    // 전체 매물 조회
    public void findAll(OnPropertiesLoaded<T> listener) {
        db.collection(collectionName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<T> properties = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        T property = doc.toObject(propertyClass);

                        // 문서 ID 설정
                        if (property != null) {
                            property.setPropertyId(doc.getId());
                            properties.add(property);
                        }
                    }
                    listener.onLoaded(properties);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding all documents in " + collectionName, e);
                    listener.onLoaded(new ArrayList<>());
                });
    }

    //  Host ID로 매물 조회
    public void findByHostId(String hostId, OnPropertiesLoaded<T> listener) {
        db.collection(collectionName)
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<T> properties = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        T property = doc.toObject(propertyClass);

                        //  문서 ID 설정
                        if (property != null) {
                            property.setPropertyId(doc.getId());
                            properties.add(property);
                        }
                    }
                    listener.onLoaded(properties);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding documents by hostId in " + collectionName, e);
                    listener.onLoaded(new ArrayList<>());
                });
    }


    public void findAllByIds(List<String> propertyIds, OnPropertiesLoaded<T> listener) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            listener.onLoaded(new ArrayList<>());
            return;
        }


        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < propertyIds.size(); i = i + 30) {
            int end = Math.min(propertyIds.size(), i + 30);
            chunks.add(propertyIds.subList(i, end));
        }

        List<T> allProperties = new ArrayList<>();

        final int[] tasksCompleted = {0};

        if (chunks.isEmpty()) {
            listener.onLoaded(new ArrayList<>());
            return;
        }

        for (List<String> chunk : chunks) {
            db.collection(collectionName)
                    .whereIn(FieldPath.documentId(), chunk) // "propertyId" 필드를 기준으로 검색
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            T property = doc.toObject(propertyClass);
                            if (property != null) {
                                property.setPropertyId(doc.getId());
                                allProperties.add(property);
                            }
                        }
                        // 모든 쿼리가 완료되면 콜백 호출
                        tasksCompleted[0]++;
                        if (tasksCompleted[0] == chunks.size()) {
                            listener.onLoaded(allProperties);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error finding documents by IDs in " + collectionName, e);
                        // 하나의 쿼리라도 실패하면 현재까지의 결과 또는 빈 리스트 반환
                        tasksCompleted[0]++;
                        if (tasksCompleted[0] == chunks.size()) {
                            listener.onLoaded(allProperties); // Or new ArrayList<>()
                        }
                    });
        }
    }

    // 매물 정보 업데이트
    public void update(String propertyId, T property, OnOperationComplete listener) {
        db.collection(collectionName)
                .document(propertyId)
                .set(property)
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating document in " + collectionName, e);
                    listener.onComplete(false);
                });
    }

    // 매물 삭제
    public void delete(String propertyId, OnOperationComplete listener) {
        db.collection(collectionName)
                .document(propertyId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting document in " + collectionName, e);
                    listener.onComplete(false);
                });
    }
    // --- 공용 콜백 인터페이스 ---
    public interface OnPropertyIdGenerated {
        void onGenerated(String propertyId);
    }

    public interface OnPropertyLoaded<T> {
        void onLoaded(T property);
    }

    public interface OnPropertiesLoaded<T> {
        void onLoaded(List<T> properties);
    }

    public interface OnOperationComplete {
        void onComplete(boolean success);
    }
}