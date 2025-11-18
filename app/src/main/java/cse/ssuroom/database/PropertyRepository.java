package cse.ssuroom.database;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 모든 매물 Repository의 공통 기능을 담는 부모 클래스입니다.
 * 제네릭 타입 T는 Property를 상속받는 클래스(LeaseTransfer, ShortTerm 등)를 의미합니다.
 */
public class PropertyRepository<T extends Property> {

    protected FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String collectionName;
    private final Class<T> propertyClass;
    private final String TAG;

    /**
     * 자식 Repository가 생성될 때, Firestore 컬렉션 이름과 클래스 타입을 받아옵니다.
     * @param collectionName Firestore에서 사용할 컬렉션의 이름 (e.g., "lease_transfers")
     * @param propertyClass 데이터를 변환할 클래스 타입 (e.g., LeaseTransfer.class)
     * @param tag Logcat에서 사용할 태그
     */
    public PropertyRepository(String collectionName, Class<T> propertyClass, String tag) {
        this.collectionName = collectionName;
        this.propertyClass = propertyClass;
        this.TAG = tag;
    }

    // C: 매물 등록 (ID 자동 생성)
    public void save(T property, OnPropertyIdGenerated listener) {
        db.collection(collectionName)
                .add(property)
                .addOnSuccessListener(docRef -> listener.onGenerated(docRef.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving document to " + collectionName, e);
                    listener.onGenerated(null);
                });
    }

    // R: 특정 매물 조회
    public void findById(String propertyId, OnPropertyLoaded<T> listener) {
        db.collection(collectionName)
                .document(propertyId)
                .get()
                .addOnSuccessListener(docSnap -> {
                    if (docSnap.exists()) {
                        T property = docSnap.toObject(propertyClass);
                        // ⭐ 문서 ID 설정
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

    // R: 전체 매물 조회
    public void findAll(OnPropertiesLoaded<T> listener) {
        db.collection(collectionName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<T> properties = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        T property = doc.toObject(propertyClass);

                        // ⭐ 문서 ID 설정
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

    // R: Host ID로 매물 조회
    public void findByHostId(String hostId, OnPropertiesLoaded<T> listener) {
        db.collection(collectionName)
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<T> properties = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        T property = doc.toObject(propertyClass);

                        // ⭐ 문서 ID 설정
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

    // U: 매물 정보 업데이트
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

    // D: 매물 삭제
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