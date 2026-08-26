package vgandolfi.dev.mana_paes.application.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserResponse user) {

    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}