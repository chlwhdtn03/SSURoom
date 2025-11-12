package cse.ssuroom.database;

/**
 * 양도 매물 정보를 담는 클래스.
 * Property 클래스를 상속받아 공통 속성을 물려받습니다.
 */
public class LeaseTransfer extends Property {

    // 양도 매물에만 필요한 필드가 있다면 여기에 추가합니다.

    public LeaseTransfer() {
        super();
    }

    /**
     * LeaseTransfer 객체를 생성하는 빌더 클래스.
     * 부모 클래스인 Property.Builder를 상속받아 공통 빌더 로직을 재사용합니다.
     */
    public static class Builder extends Property.Builder<LeaseTransfer, Builder> {

        // LeaseTransfer.Builder에만 필요한 추가적인 setter가 있다면 여기에 추가합니다.

        @Override
        public LeaseTransfer build() {
            LeaseTransfer lease = new LeaseTransfer();
            // 부모 빌더의 build() 메소드를 호출하여 공통 속성을 설정합니다.
            super.build(lease);

            // 자식 클래스 고유의 속성을 설정합니다.

            return lease;
        }
    }
}
