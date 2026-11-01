package com.example.cinema_middleware.v1.domain.entity.enums;

/**
 * 상영 타입. DB 에는 키(name)를 저장하고, 응답에는 displayName 을 내보낸다.
 */
public enum ScreenType {

    V_2D("2D"), V_4D("4D"), V_SCREENX("SCREENX");

    private final String displayName;

    ScreenType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
