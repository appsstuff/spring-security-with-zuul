package com.f1soft.profileservice.service;

import com.f1soft.profileservice.entities.Profile;
import com.f1soft.profileservice.entities.ProfileMenu;
import com.f1soft.profileservice.exceptions.DataDuplicationException;
import com.f1soft.profileservice.exceptions.NoContentFoundException;
import com.f1soft.profileservice.repository.ProfileMenuRepository;
import com.f1soft.profileservice.repository.ProfileRepository;
import com.f1soft.profileservice.requestDTO.ProfileRequestDTO;
import com.f1soft.profileservice.service.serviceImpl.ProfileMenuServiceImpl;
import com.f1soft.profileservice.service.serviceImpl.ProfileServiceImpl;
import com.f1soft.profileservice.utility.ProfileMenuUtils;
import com.f1soft.profileservice.utility.ProfileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static com.f1soft.profileservice.utils.ProfileRequestUtils.*;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author smriti on 7/2/19
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfileServiceImplTest {

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileMenuServiceImpl profileMenuService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {

    }

    @Test
    public void saveProfileTest() {
        Should_ThrowException_When_ProfileNameExists();

        Should_ThrowException_When_UserMenusIsEmpty();

        Should_Successfully_SaveProfile();

        Should_Successfully_SaveProfileMenu();
    }

    @Test
    public void updateProfileTest() {
        Should_ThrowException_When_ProfileIsNotFound();

        Should_ThrowException_When_UserMenusIsEmpty();

        Should_Throw_Exception_When_ProfileNameAlreadyExists();

        Should_Successfully_UpdateProfile();

        Should_Successfully_UpdateProfileMenu();
    }

    @Test
    public void deleteProfile() {
        Should_ThrowException_When_ProfileIsNotFound();
    }

    @Test
    public void Should_ThrowException_When_ProfileNameExists() {

        ProfileRequestDTO requestDTO = getProfileRequestDTOThatThrowsException();

        System.out.println(profileRepository.findProfileByName(requestDTO.getProfileDTO().getName()));

        given(profileRepository.findProfileByName(requestDTO.getProfileDTO().getName())).willReturn(ONE);

        thrown.expect(DataDuplicationException.class);

        profileService.createProfile(requestDTO);
    }

    @Test
    public void Should_ThrowException_When_UserMenusIsEmpty() {

        ProfileRequestDTO profileRequestDTO = getProfileRequestDTOThatThrowsException();

        thrown.expect(NoContentFoundException.class);

        profileService.createProfile(profileRequestDTO);
    }

    @Test
    public void Should_Successfully_SaveProfile() {

        ProfileRequestDTO requestDTO = getProfileRequestDTO();

        Profile expected = ProfileUtils.convertToProfileInfo(requestDTO.getProfileDTO());

        saveProfile(requestDTO, expected);

        profileService.createProfile(requestDTO);

        verify(profileRepository, times(1)).save(expected);
    }

    public void saveProfile(ProfileRequestDTO requestDTO, Profile expected) {
        given(profileRepository.findProfileByName(requestDTO.getProfileDTO().getName())).willReturn(ZERO);

        given(profileRepository.save(expected)).willReturn(getProfileInfo());
    }

    @Test
    public void Should_Successfully_SaveProfileMenu() {
        ProfileRequestDTO requestDTO = getProfileRequestDTO();

        Profile profile = getProfileInfo();

        saveProfile(requestDTO, ProfileUtils.convertToProfileInfo(requestDTO.getProfileDTO()));

        List<ProfileMenu> expectedProfileMenus = ProfileMenuUtils.convertToProfileMenu(profile.getId(),
                requestDTO.getProfileMenuRequestDTO());

        given(profileMenuService.saveProfileMenu(expectedProfileMenus)).willReturn(getProfileMenu());

        profileService.createProfile(requestDTO);

        assertThat(profileMenuService.saveProfileMenu(expectedProfileMenus),
                hasSize(requestDTO.getProfileMenuRequestDTO().size()));

        assertThat(profileMenuService.saveProfileMenu(expectedProfileMenus).get(0).getRoleId())
                .isEqualTo(requestDTO.getProfileMenuRequestDTO().get(0).getRoleId());
    }

    @Test
    public void Should_ThrowException_When_ProfileIsNotFound() {

        ProfileRequestDTO requestDTO = getProfileRequestDTOForUpdate();

        given(profileRepository.findById(requestDTO.getProfileDTO().getId())).willReturn(Optional.ofNullable(null));

        thrown.expect(NoContentFoundException.class);

        profileService.updateProfile(requestDTO);
    }

    @Test
    public void Should_Throw_Exception_When_ProfileNameAlreadyExists() {

        ProfileRequestDTO requestDTO = getProfileRequestDTOForUpdate();

        given(profileRepository.findById(requestDTO.getProfileDTO().getId())).willReturn(Optional.of(getProfileInfo()));

        given(profileRepository.findProfileByIdAndName(requestDTO.getProfileDTO().getId(),
                requestDTO.getProfileDTO().getName())).willReturn(ONE);

        thrown.expect(DataDuplicationException.class);

        profileService.updateProfile(requestDTO);
    }

    @Test
    public void Should_Successfully_UpdateProfile() {
        ProfileRequestDTO requestDTO = getProfileRequestDTOForUpdate();

        Profile savedProfile = getProfileInfo();

        Profile updatedProfile = ProfileUtils.convertToProfileEntity.apply(requestDTO.getProfileDTO(), savedProfile);

        updateProfile(requestDTO, savedProfile, updatedProfile);

        profileService.updateProfile(requestDTO);

        verify(profileRepository, times(1)).save(updatedProfile);
    }

    public void updateProfile(ProfileRequestDTO requestDTO, Profile savedProfile, Profile updatedProfile) {

        given(profileRepository.findById(requestDTO.getProfileDTO().getId())).willReturn(Optional.of(savedProfile));

        given(profileRepository.findProfileByIdAndName(requestDTO.getProfileDTO().getId(),
                requestDTO.getProfileDTO().getName())).willReturn(ZERO);

        given(profileRepository.save(updatedProfile)).willReturn(getUpdatedProfileInfo());
    }

    @Test
    public void Should_Successfully_UpdateProfileMenu() {

        ProfileRequestDTO requestDTO = getProfileRequestDTOForUpdate();

        Profile savedProfile = getProfileInfo();

        updateProfile(requestDTO, savedProfile, ProfileUtils.convertToProfileEntity.apply(requestDTO.getProfileDTO(),
                savedProfile));

        List<ProfileMenu> expectedProfileMenus = ProfileMenuUtils.convertToProfileMenu(savedProfile.getId(),
                requestDTO.getProfileMenuRequestDTO());

        given(profileMenuService.saveProfileMenu(expectedProfileMenus)).willReturn(getUpdatedProfileMenus());

        profileService.updateProfile(requestDTO);

        assertThat(profileMenuService.saveProfileMenu(expectedProfileMenus),
                hasSize(requestDTO.getProfileMenuRequestDTO().size()));

        assertThat(profileMenuService.saveProfileMenu(expectedProfileMenus).get(0).getStatus())
                .isEqualTo(requestDTO.getProfileMenuRequestDTO().get(0).getStatus());
    }

    @Test
    public void Should_ThrowException_When_ProfileIsNull() {
        Long id = 1L;

        given(profileRepository.findById(id)).willReturn(Optional.ofNullable(null));

        thrown.expect(NoContentFoundException.class);

        profileService.deleteProfile(id);
    }

    @Test
    public void Should_SuccessFully_DeleteProfile() {
        Long id = 1L;

        Profile profile = getProfileInfo();

        given(profileRepository.findById(id)).willReturn(Optional.of(profile));

        profile.setStatus('D');

        given(profileRepository.save(profile)).willReturn(getDeletedProfileInfo());

        profileService.deleteProfile(id);

        assertThat(profileRepository.save(profile).getStatus()).isEqualTo('D');
    }


}
