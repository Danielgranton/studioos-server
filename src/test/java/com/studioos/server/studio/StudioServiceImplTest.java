package com.studioos.server.studio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.beatmarketplace.BeatRepository;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.payment.TransactionRepository;
import com.studioos.server.payment.WithdrawalRepository;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.dto.StudioResponse;
import com.studioos.server.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StudioServiceImplTest {

    @Mock
    private StudioRepository studioRepository;
    @Mock
    private StudioRatingRepository ratingRepository;
    @Mock
    private BeatRepository beatRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AdCampaignRepository adCampaignRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WithdrawalRepository withdrawalRepository;

    @InjectMocks
    private StudioServiceImpl studioService;

    @Test
    void getAllStudiosUsesPaginatedSpecificationQuery() {
        Studio studioA = Studio.builder()
                .id("studio-a")
                .studioName("Studio A")
                .location("Nairobi")
                .pricing(2000)
                .availability("Open")
                .description("Desc")
                .ownerId(1)
                .build();
        Studio studioB = Studio.builder()
                .id("studio-b")
                .studioName("Studio B")
                .location("Nairobi")
                .pricing(1500)
                .availability("Open")
                .description("Desc")
                .ownerId(2)
                .build();

        Page<Studio> page = new PageImpl<>(List.of(studioA, studioB), PageRequest.of(0, 10), 2);
        when(studioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(ratingRepository.findAverageRatingByStudioId(anyString())).thenReturn(4.5);
        when(ratingRepository.countByStudioId(anyString())).thenReturn(3L);

        PageResponse<StudioResponse> response = studioService.getAllStudios("Nairobi", 2500, 0, 10);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(studioRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by("createdAt").descending());
    }

    @Test
    void deleteStudioRejectsWhenBeatsExist() {
        Studio studio = Studio.builder()
                .id("studio-a")
                .studioName("Studio A")
                .location("Nairobi")
                .availability("Open")
                .description("Desc")
                .ownerId(1)
                .build();
        User owner = User.builder().id(1).role(Role.PRODUCER).email("owner@example.com").name("Owner").build();

        when(studioRepository.findById("studio-a")).thenReturn(Optional.of(studio));
        when(beatRepository.existsByStudioId("studio-a")).thenReturn(true);

        assertThatThrownBy(() -> studioService.deleteStudio(owner, "studio-a"))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("beats");
    }
}
