package uk.gov.moj.cpp.sjp.query.view.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
