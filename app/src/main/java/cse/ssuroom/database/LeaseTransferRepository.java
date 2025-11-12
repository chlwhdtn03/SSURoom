package cse.ssuroom.database;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LeaseTransferRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();


    // property id 자동 생성
    public void saveLeaseTransferAutoId(LeaseTransfer lease, OnPropertyIdGenerated listener) {
        db.collection("lease_transfers")
                .add(lease)
                .addOnSuccessListener(docRef -> listener.onGenerated(docRef.getId()))
                .addOnFailureListener(e -> listener.onGenerated(null));
    }

    // 특정 매물 조회
    public void getLeaseTransfer(String propertyId, OnLeaseTransferLoaded listener) {
        db.collection("lease_transfers")
                .document(propertyId)
                .get()
                .addOnSuccessListener(docSnap -> {
                    LeaseTransfer lease = docSnap.toObject(LeaseTransfer.class);
                    listener.onLoaded(lease);
                });
    }


    // 전체 매물 조회
    public void getAllLeases(OnLeasesLoaded listener) {
        db.collection("lease_transfers")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<LeaseTransfer> leases = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        leases.add(doc.toObject(LeaseTransfer.class));
                    }
                    listener.onLoaded(leases);
                });
    }

    // 조건 검색(필터때 사용하면 좋을듯) 지금은 host id로만 되어있음
    public void getLeasesByHost(String hostId, OnLeasesLoaded listener) {
        db.collection("lease_transfers")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<LeaseTransfer> leases = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        leases.add(doc.toObject(LeaseTransfer.class));
                    }
                    listener.onLoaded(leases);
                });
    }
    // 업데이트
    public void updateLeaseTransfer(String propertyId, LeaseTransfer updatedLease, OnOperationComplete listener) {
        db.collection("lease_transfers")
                .document(propertyId)
                .set(updatedLease)
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> listener.onComplete(false));
    }
    // 삭제
    public void deleteLeaseTransfer(String propertyId, OnOperationComplete listener) {
        db.collection("lease_transfers")
                .document(propertyId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> listener.onComplete(false));
    }






    // 콜백 인터페이스
    public interface OnLeaseTransferLoaded {
        void onLoaded(LeaseTransfer lease);
    }

    public interface OnPropertyIdGenerated {
        void onGenerated(String propertyId);
    }

    public interface OnLeasesLoaded {
        void onLoaded(List<LeaseTransfer> leases);
    }

    public interface OnOperationComplete {
        void onComplete(boolean success);
    }
}
