package uk.gov.moj.cpp.sjp.filestore.azure;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.UtcClock;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class SasUriGeneratorTest {

    private static final ZonedDateTime FIXED_NOW = ZonedDateTime.of(2026, 5, 19, 12, 0, 0, 0, UTC);
    private static final OffsetDateTime FIXED_NOW_OFFSET = FIXED_NOW.toOffsetDateTime();
    private static final OffsetDateTime EXPECTED_SAS_EXPIRY = FIXED_NOW_OFFSET.plusHours(24);
    private static final OffsetDateTime EXPECTED_DELEGATION_KEY_EXPIRY = FIXED_NOW_OFFSET.plusHours(25);

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private AzureBlobConfiguration azureBlobConfiguration;

    @Mock
    private UtcClock utcClock;

    @Mock
    private Logger logger;

    @Mock
    private BlobClient blobClient;

    @InjectMocks
    private SasUriGenerator sasUriGenerator;

    @Test
    public void shouldGenerateAccountSasWhenConnectionStringPresent() {
        final UUID fileId = randomUUID();
        final ArgumentCaptor<BlobServiceSasSignatureValues> sasValuesCaptor =
                ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        when(utcClock.now()).thenReturn(FIXED_NOW);
        when(azureBlobConfiguration.getSasExpiry()).thenReturn(Duration.ofHours(24));
        when(azureBlobConfiguration.hasConnectionString()).thenReturn(true);
        when(blobContainerClient.getBlobClient("published/transparency-reports/" + fileId)).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/container/published/transparency-reports/" + fileId);
        when(blobClient.generateSas(sasValuesCaptor.capture())).thenReturn("sv=2021-08-06&sp=r&sig=abc");

        final URI uri = sasUriGenerator.generateReadUri(StoragePath.published("transparency-reports"), fileId);

        assertThat(uri, notNullValue());
        assertThat(uri.toString(), containsString("sv=2021-08-06"));
        assertThat(sasValuesCaptor.getValue().getExpiryTime(), is(EXPECTED_SAS_EXPIRY));
    }

    @Test
    public void shouldGenerateUserDelegationSasWhenNoConnectionString() {
        final UserDelegationKey userDelegationKey = mock(UserDelegationKey.class);
        final UUID fileId = randomUUID();
        final ArgumentCaptor<BlobServiceSasSignatureValues> sasValuesCaptor =
                ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        final ArgumentCaptor<UserDelegationKey> delegationKeyCaptor =
                ArgumentCaptor.forClass(UserDelegationKey.class);
        when(utcClock.now()).thenReturn(FIXED_NOW);
        when(azureBlobConfiguration.getSasExpiry()).thenReturn(Duration.ofHours(24));
        when(azureBlobConfiguration.hasConnectionString()).thenReturn(false);
        when(blobContainerClient.getBlobClient("published/transparency-reports/" + fileId)).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/container/published/transparency-reports/" + fileId);
        when(blobServiceClient.getUserDelegationKey(FIXED_NOW_OFFSET, EXPECTED_DELEGATION_KEY_EXPIRY)).thenReturn(userDelegationKey);
        when(blobClient.generateUserDelegationSas(sasValuesCaptor.capture(), delegationKeyCaptor.capture())).thenReturn("sv=2021-08-06&sp=r&skoid=abc");

        final URI uri = sasUriGenerator.generateReadUri(StoragePath.published("transparency-reports"), fileId);

        assertThat(uri, notNullValue());
        assertThat(uri.toString(), containsString("skoid=abc"));
        assertThat(sasValuesCaptor.getValue().getExpiryTime(), is(EXPECTED_SAS_EXPIRY));
        assertThat(delegationKeyCaptor.getValue(), is(userDelegationKey));
    }

    @Test
    public void shouldReturnCachedDelegationKeyOnSubsequentCall() {
        final UserDelegationKey userDelegationKey = mock(UserDelegationKey.class);
        final UUID fileId1 = randomUUID();
        final UUID fileId2 = randomUUID();
        when(utcClock.now()).thenReturn(FIXED_NOW);
        when(azureBlobConfiguration.getSasExpiry()).thenReturn(Duration.ofHours(24));
        when(azureBlobConfiguration.getDelegationKeyRefreshThreshold()).thenReturn(Duration.ofMinutes(15));
        when(azureBlobConfiguration.hasConnectionString()).thenReturn(false);
        when(blobContainerClient.getBlobClient("published/transparency-reports/" + fileId1)).thenReturn(blobClient);
        when(blobContainerClient.getBlobClient("published/transparency-reports/" + fileId2)).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/container/blob");
        when(blobServiceClient.getUserDelegationKey(FIXED_NOW_OFFSET, EXPECTED_DELEGATION_KEY_EXPIRY)).thenReturn(userDelegationKey);

        sasUriGenerator.generateReadUri(StoragePath.published("transparency-reports"), fileId1);
        sasUriGenerator.generateReadUri(StoragePath.published("transparency-reports"), fileId2);

        verify(blobServiceClient).getUserDelegationKey(FIXED_NOW_OFFSET, EXPECTED_DELEGATION_KEY_EXPIRY);
    }
}
