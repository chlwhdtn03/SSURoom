package cse.ssuroom.database;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ShortTermRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "ShortTermRepo";

    // property id 자동 생성
    public void saveShortTermAutoId(ShortTerm shortTerm, OnPropertyIdGenerated listener) {
        db.collection("short_terms")
                .add(shortTerm)
                .addOnSuccessListener(docRef -> listener.onGenerated(docRef.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving short term", e);
                    listener.onGenerated(null);
                });
    }

    // 특정 매물 조회
    public void getShortTerm(String propertyId, OnShortTermLoaded listener) {
        db.collection("short_terms")
                .document(propertyId)
                .get()
                .addOnSuccessListener(docSnap -> {
                    if (docSnap.exists()) {
                        ShortTerm shortTerm = docSnap.toObject(ShortTerm.class);
                        listener.onLoaded(shortTerm);
                    } else {
                        listener.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting short term", e);
                    listener.onLoaded(null);
                });
    }

    // 전체 매물 조회
    public void getAllShortTerms(OnShortTermsLoaded listener) {
        db.collection("short_terms")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShortTerm> shortTerms = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        shortTerms.add(doc.toObject(ShortTerm.class));
                    }
                    listener.onLoaded(shortTerms);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting all short terms", e);
                    listener.onLoaded(new ArrayList<>()); // Return empty list on failure
                });
    }

    // 조건 검색 (host id로만 되어있음)
    public void getShortTermsByHost(String hostId, OnShortTermsLoaded listener) {
        db.collection("short_terms")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShortTerm> shortTerms = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        shortTerms.add(doc.toObject(ShortTerm.class));
                    }
                    listener.onLoaded(shortTerms);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting short terms by host", e);
                    listener.onLoaded(new ArrayList<>());
                });
    }

    // 업데이트
    public void updateShortTerm(String propertyId, ShortTerm updatedShortTerm, OnOperationComplete listener) {
        db.collection("short_terms")
                .document(propertyId)
                .set(updatedShortTerm)
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating short term", e);
                    listener.onComplete(false);
                });
    }

    // 삭제
    public void deleteShortTerm(String propertyId, OnOperationComplete listener) {
        db.collection("short_terms")
                .document(propertyId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting short term", e);
                    listener.onComplete(false);
                });
    }

    // 콜백 인터페이스
    public interface OnShortTermLoaded {
        void onLoaded(ShortTerm shortTerm);
    }

    public interface OnPropertyIdGenerated {
        void onGenerated(String propertyId);
    }

    public interface OnShortTermsLoaded {
        void onLoaded(List<ShortTerm> shortTerms);
    }

    public interface OnOperationComplete {
        void onComplete(boolean success);
    }
}
