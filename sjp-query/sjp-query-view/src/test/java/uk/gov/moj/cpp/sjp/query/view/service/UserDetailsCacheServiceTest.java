package uk.gov.moj.cpp.sjp.query.view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserDetailsCacheServiceTest {

    @Mock
    private UserAndGroupsService userAndGroupsService;

    @InjectMocks
    private UserDetailsCacheService userDetailsCacheService;

    @Test
    public void shouldGetSavedValue() {
        final UUID userId = UUID.randomUUID();
        final String userDetails = " TEST USER";
        Mockito.when(userAndGroupsService.getUserDetails(userId, null)).thenReturn(userDetails);
        assertNotNull(userDetailsCacheService.getUserName(null, userId));
        assertEquals(userDetailsCacheService.getUserName(null, userId), userDetails);
    }

}
