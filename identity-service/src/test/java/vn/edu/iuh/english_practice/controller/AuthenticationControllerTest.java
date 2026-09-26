package vn.edu.iuh.english_practice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import vn.edu.iuh.english_practice.dto.request.IntropectTokenRequest;
import vn.edu.iuh.english_practice.service.AuthenticationService;
import vn.edu.iuh.english_practice.service.InvalidatedTokenService;

class AuthenticationControllerTest {

    @Test
    void returnsValidationResultForGateway() throws Exception {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        InvalidatedTokenService invalidatedTokenService = mock(InvalidatedTokenService.class);
        AuthenticationController controller =
                new AuthenticationController(authenticationService, invalidatedTokenService);
        IntropectTokenRequest request = new IntropectTokenRequest("valid-token");
        when(authenticationService.intropectToken(request)).thenReturn(true);

        var response = controller.introspect(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResult()).isTrue();
    }
}
