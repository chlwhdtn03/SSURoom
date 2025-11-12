package cse.ssuroom.database;

/**
 * 단기 매물(ShortTerm)에 대한 데이터베이스 처리를 담당하는 클래스.
 * 공통 로직은 PropertyRepository로부터 상속받습니다.
 */
public class ShortTermRepository extends PropertyRepository<ShortTerm> {

    /**
     * 부모 클래스인 PropertyRepository의 생성자를 호출합니다.
     * Firestore 컬렉션 이름, 클래스 타입, 그리고 로그 태그를 전달합니다.
     */
    public ShortTermRepository() {
        super("short_terms", ShortTerm.class, "ShortTermRepo");
    }

    // ShortTerm에만 필요한 특별한 데이터베이스 쿼리가 있다면 여기에 추가할 수 있습니다.

}
