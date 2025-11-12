package cse.ssuroom.database;

/**
 * 양도 매물(LeaseTransfer)에 대한 데이터베이스 처리를 담당하는 클래스.
 * 공통 로직은 PropertyRepository로부터 상속받습니다.
 */
public class LeaseTransferRepository extends PropertyRepository<LeaseTransfer> {

    /**
     * 부모 클래스인 PropertyRepository의 생성자를 호출합니다.
     * Firestore 컬렉션 이름, 클래스 타입, 그리고 로그 태그를 전달합니다.
     */
    public LeaseTransferRepository() {
        super("lease_transfers", LeaseTransfer.class, "LeaseTransferRepo");
    }

    // LeaseTransfer에만 필요한 특별한 데이터베이스 쿼리가 있다면 여기에 추가할 수 있습니다.
    // 예를 들어, "특정 가격대 이상의 양도 매물만 검색" 같은 기능입니다.

}
