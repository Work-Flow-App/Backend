package com.workflow.service.paddle;

import com.workflow.common.constant.PlanType;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.dto.paddle.GenerateCheckoutLinkRequest;
import com.workflow.dto.paddle.PaddleSubscriptionResponse;
import com.workflow.dto.paddle.PaddleSubscriptionUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers PaddleService.updateSubscriptionAddons and the buildLineItems logic it (and
 * generateCheckoutUrl) shares — tested indirectly here since buildLineItems is private, by
 * capturing the PaddleSubscriptionUpdateRequest body sent to the mocked RestClient.
 */
@ExtendWith(MockitoExtension.class)
class PaddleServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec bodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec bodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private PaddleConfigProperties paddleProps;

    private PaddleService paddleService;

    @BeforeEach
    void setUp() {
        paddleService = new PaddleService(restClient, paddleProps);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<PaddleSubscriptionUpdateRequest> stubPatchChain(PaddleSubscriptionResponse toReturn) {
        when(restClient.patch()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString(), any(Object.class))).thenReturn(bodySpec);
        ArgumentCaptor<PaddleSubscriptionUpdateRequest> captor =
                ArgumentCaptor.forClass(PaddleSubscriptionUpdateRequest.class);
        when(bodySpec.body(captor.capture())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PaddleSubscriptionResponse.class)).thenReturn(toReturn);
        return captor;
    }

    @Test
    void updateSubscriptionAddons_BothAddonsPositive_BuildsFullItemsListAndPatchesSubscription() {
        when(paddleProps.basePriceIdFor(PlanType.STARTER)).thenReturn("pri_base_starter");
        when(paddleProps.extraSeatPriceIdFor(PlanType.STARTER)).thenReturn("pri_seat_starter");
        when(paddleProps.storageBlockPriceIdFor(PlanType.STARTER)).thenReturn("pri_storage_starter");
        PaddleSubscriptionResponse expected = new PaddleSubscriptionResponse(null);
        ArgumentCaptor<PaddleSubscriptionUpdateRequest> captor = stubPatchChain(expected);

        PaddleSubscriptionResponse result =
                paddleService.updateSubscriptionAddons("sub_123", PlanType.STARTER, 5, 3);

        assertThat(result).isSameAs(expected);
        verify(bodyUriSpec).uri("/subscriptions/{id}", "sub_123");

        PaddleSubscriptionUpdateRequest sent = captor.getValue();
        assertThat(sent.prorationBillingMode()).isEqualTo("prorated_immediately");
        assertThat(sent.items()).containsExactly(
                new GenerateCheckoutLinkRequest.CheckoutItem("pri_base_starter", 1),
                new GenerateCheckoutLinkRequest.CheckoutItem("pri_seat_starter", 5),
                new GenerateCheckoutLinkRequest.CheckoutItem("pri_storage_starter", 3)
        );
    }

    @Test
    void updateSubscriptionAddons_BothAddonsZero_OnlyBasePlanItemIncluded() {
        when(paddleProps.basePriceIdFor(PlanType.PROFESSIONAL)).thenReturn("pri_base_pro");
        ArgumentCaptor<PaddleSubscriptionUpdateRequest> captor =
                stubPatchChain(new PaddleSubscriptionResponse(null));

        paddleService.updateSubscriptionAddons("sub_456", PlanType.PROFESSIONAL, 0, 0);

        assertThat(captor.getValue().items()).containsExactly(
                new GenerateCheckoutLinkRequest.CheckoutItem("pri_base_pro", 1)
        );
        verify(paddleProps, never()).extraSeatPriceIdFor(any());
        verify(paddleProps, never()).storageBlockPriceIdFor(any());
    }

    @Test
    void updateSubscriptionAddons_OnlySeatsPositive_OmitsStorageItem() {
        when(paddleProps.basePriceIdFor(PlanType.STARTER)).thenReturn("pri_base_starter");
        when(paddleProps.extraSeatPriceIdFor(PlanType.STARTER)).thenReturn("pri_seat_starter");
        ArgumentCaptor<PaddleSubscriptionUpdateRequest> captor =
                stubPatchChain(new PaddleSubscriptionResponse(null));

        paddleService.updateSubscriptionAddons("sub_789", PlanType.STARTER, 4, 0);

        assertThat(captor.getValue().items()).containsExactly(
                new GenerateCheckoutLinkRequest.CheckoutItem("pri_base_starter", 1),
                new GenerateCheckoutLinkRequest.CheckoutItem("pri_seat_starter", 4)
        );
        verify(paddleProps, never()).storageBlockPriceIdFor(any());
    }

    @Test
    void updateSubscriptionAddons_OnlyStoragePositive_OmitsSeatItem() {
        when(paddleProps.basePriceIdFor(PlanType.PROFESSIONAL)).thenReturn("pri_base_pro");
        when(paddleProps.storageBlockPriceIdFor(PlanType.PROFESSIONAL)).thenReturn("pri_storage_pro");
        ArgumentCaptor<PaddleSubscriptionUpdateRequest> captor =
                stubPatchChain(new PaddleSubscriptionResponse(null));

        paddleService.updateSubscriptionAddons("sub_999", PlanType.PROFESSIONAL, 0, 2);

        assertThat(captor.getValue().items()).containsExactly(
                new GenerateCheckoutLinkRequest.CheckoutItem("pri_base_pro", 1),
                new GenerateCheckoutLinkRequest.CheckoutItem("pri_storage_pro", 2)
        );
        verify(paddleProps, never()).extraSeatPriceIdFor(any());
    }
}
