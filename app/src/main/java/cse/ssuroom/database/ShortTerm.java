package cse.ssuroom.database;

/**
 * 단기 매물 정보를 담는 클래스.
 * Property 클래스를 상속받아 공통 속성을 물려받습니다.
 */
public class ShortTerm extends Property {

    // 단기 매물에만 필요한 필드가 있다면 여기에 추가합니다.
    // 예를 들어, private int minStayDays;

    public ShortTerm() {
        super();
    }

    /**
     * ShortTerm 객체를 생성하는 빌더 클래스.
     * 부모 클래스인 Property.Builder를 상속받아 공통 빌더 로직을 재사용합니다.
     */
    public static class Builder extends Property.Builder<ShortTerm, Builder> {

        // ShortTerm.Builder에만 필요한 추가적인 setter가 있다면 여기에 추가합니다.
        // public Builder setMinStayDays(int days) { ... }

        @Override
        public ShortTerm build() {
            ShortTerm shortTerm = new ShortTerm();
            // 부모 빌더의 build() 메소드를 호출하여 공통 속성을 설정합니다.
            super.build(shortTerm);

            // 자식 클래스 고유의 속성을 설정합니다.
            // shortTerm.setMinStayDays(this.minStayDays);

            return shortTerm;
        }
    }
}
